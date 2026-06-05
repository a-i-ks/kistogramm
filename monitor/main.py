import asyncio
import logging
import os
import re
from pathlib import Path

import docker
import httpx
import psutil
import redis.asyncio as aioredis
from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import FileResponse, HTMLResponse, JSONResponse, StreamingResponse
from starlette.responses import Response

log = logging.getLogger(__name__)
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")

APP_URL = os.getenv("APP_URL", "http://app:8080")
REDIS_HOST = os.getenv("REDIS_HOST", "redis")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
OLLAMA_URL = os.getenv("OLLAMA_URL", "http://host.docker.internal:11434")
WEBHOOK_SECRET = os.getenv("WEBHOOK_SECRET", "")
QUEUE_KEY = "ai_jobs_queue"

OLLAMA_LOG_FILE = Path(os.getenv("OLLAMA_LOG_FILE", "/var/log/ollama.log"))

CONTAINER_MAP = {
    "app": "kistogramm_app",
    "db": "kistogramm_db",
    "redis": "kistogramm_redis",
    "ai-worker": "kistogramm_ai_worker",
}

app = FastAPI()


@app.get("/", response_class=HTMLResponse)
async def dashboard():
    return Path("static/index.html").read_text()


# ── Status ──────────────────────────────────────────────────────────────────

async def _check_backend() -> dict:
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{APP_URL}/actuator/health", timeout=3.0)
        body = resp.json()
        db_status = (
            body.get("components", {}).get("db", {}).get("status", "UNKNOWN").lower()
        )
        return {
            "backend": {"status": "up" if body.get("status") == "UP" else "down"},
            "db": {"status": "up" if db_status == "up" else "down"},
        }
    except Exception as e:
        return {"backend": {"status": "down", "detail": str(e)}, "db": {"status": "unknown"}}


async def _check_redis() -> dict:
    try:
        r = aioredis.Redis(host=REDIS_HOST, port=REDIS_PORT, socket_timeout=3)
        await r.ping()
        depth = await r.llen(QUEUE_KEY)
        await r.aclose()
        return {"status": "up", "queue_depth": depth}
    except Exception as e:
        return {"status": "down", "detail": str(e), "queue_depth": 0}


async def _check_ollama() -> dict:
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{OLLAMA_URL}/api/tags", timeout=3.0)
        resp.raise_for_status()
        return {"status": "up"}
    except Exception as e:
        return {"status": "down", "detail": str(e)}


@app.get("/api/status")
async def get_status():
    backend_db, redis_result, ollama_result = await asyncio.gather(
        _check_backend(),
        _check_redis(),
        _check_ollama(),
        return_exceptions=True,
    )
    if isinstance(backend_db, Exception):
        backend_db = {"backend": {"status": "down"}, "db": {"status": "unknown"}}
    if isinstance(redis_result, Exception):
        redis_result = {"status": "down", "queue_depth": 0}
    if isinstance(ollama_result, Exception):
        ollama_result = {"status": "down"}

    return {
        "backend": backend_db["backend"],
        "db": backend_db["db"],
        "redis": redis_result,
        "ollama": ollama_result,
    }


# ── System metrics ───────────────────────────────────────────────────────────

def _parse_docker_cpu(stats: dict) -> float:
    try:
        cpu = stats["cpu_stats"]
        precpu = stats["precpu_stats"]
        delta = cpu["cpu_usage"]["total_usage"] - precpu["cpu_usage"]["total_usage"]
        sys_delta = cpu["system_cpu_usage"] - precpu["system_cpu_usage"]
        num_cpus = cpu.get("online_cpus") or len(cpu["cpu_usage"].get("percpu_usage", [1]))
        return round((delta / sys_delta) * num_cpus * 100.0, 1) if sys_delta > 0 else 0.0
    except (KeyError, ZeroDivisionError):
        return 0.0


def _parse_docker_mem(stats: dict) -> int:
    try:
        usage = stats["memory_stats"]["usage"]
        cache = stats["memory_stats"].get("stats", {}).get("cache", 0)
        return max(0, (usage - cache) // (1024 * 1024))
    except KeyError:
        return 0


def _container_stats() -> dict:
    result = {}
    try:
        client = docker.DockerClient(base_url="unix:///var/run/docker.sock")
        for name, cname in CONTAINER_MAP.items():
            try:
                container = client.containers.get(cname)
                stats = container.stats(stream=False)
                result[name] = {
                    "cpu_percent": _parse_docker_cpu(stats),
                    "memory_mb": _parse_docker_mem(stats),
                    "status": container.status,
                }
            except Exception:
                result[name] = None
        client.close()
    except Exception as e:
        log.warning("Docker stats unavailable: %s", e)
    return result


async def _ollama_models() -> list:
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(f"{OLLAMA_URL}/api/ps", timeout=3.0)
        models = resp.json().get("models", [])
        return [
            {
                "name": m.get("name", ""),
                "size_gb": round(m.get("size", 0) / 1e9, 2),
                "size_vram_gb": round(m.get("size_vram", 0) / 1e9, 2),
                "on_gpu": m.get("size_vram", 0) > 0,
            }
            for m in models
        ]
    except Exception:
        return []


@app.get("/api/system")
async def get_system():
    try:
        with open("/proc/uptime") as f:
            uptime_seconds = int(float(f.read().split()[0]))
    except OSError:
        uptime_seconds = int(psutil.time.time() - psutil.boot_time())

    cpu_percent = psutil.cpu_percent(interval=0.3)
    load_avg = list(psutil.getloadavg())
    mem = psutil.virtual_memory()

    container_stats, ollama_models = await asyncio.gather(
        asyncio.get_event_loop().run_in_executor(None, _container_stats),
        _ollama_models(),
    )

    return {
        "uptime_seconds": uptime_seconds,
        "cpu_percent": round(cpu_percent, 1),
        "load_avg": [round(x, 2) for x in load_avg],
        "memory": {
            "total_gb": round(mem.total / 1e9, 1),
            "used_gb": round(mem.used / 1e9, 1),
            "percent": mem.percent,
        },
        "ollama": {"models": ollama_models},
        "containers": container_stats,
    }


# ── Jobs proxy ───────────────────────────────────────────────────────────────

@app.get("/api/jobs")
async def proxy_jobs(request: Request):
    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(
                f"{APP_URL}/api/ai/jobs",
                params=dict(request.query_params),
                timeout=5.0,
            )
        return JSONResponse(content=resp.json(), status_code=resp.status_code)
    except Exception as e:
        return JSONResponse({"error": str(e), "jobs": []}, status_code=502)


@app.post("/api/jobs/{job_id}/pause")
async def pause_job(job_id: str):
    async with httpx.AsyncClient() as client:
        resp = await client.post(f"{APP_URL}/api/ai/jobs/{job_id}/pause", timeout=5.0)
    return JSONResponse(content=resp.json(), status_code=resp.status_code)


@app.post("/api/jobs/{job_id}/resume")
async def resume_job(job_id: str):
    async with httpx.AsyncClient() as client:
        resp = await client.post(f"{APP_URL}/api/ai/jobs/{job_id}/resume", timeout=5.0)
    return JSONResponse(content=resp.json(), status_code=resp.status_code)


@app.delete("/api/jobs")
async def delete_jobs_bulk(request: Request):
    async with httpx.AsyncClient() as client:
        resp = await client.delete(
            f"{APP_URL}/api/ai/jobs",
            params=dict(request.query_params),
            timeout=10.0,
        )
    return JSONResponse(content=resp.json(), status_code=resp.status_code)


@app.delete("/api/jobs/{job_id}")
async def delete_job(job_id: str):
    async with httpx.AsyncClient() as client:
        resp = await client.delete(f"{APP_URL}/api/ai/jobs/{job_id}", timeout=5.0)
    return Response(status_code=resp.status_code)


# ── Settings proxy ───────────────────────────────────────────────────────────

@app.get("/api/settings")
async def get_settings():
    async with httpx.AsyncClient() as client:
        resp = await client.get(f"{APP_URL}/api/settings", timeout=5.0)
    return JSONResponse(content=resp.json(), status_code=resp.status_code)


@app.put("/api/settings")
async def put_settings(request: Request):
    body = await request.json()
    async with httpx.AsyncClient() as client:
        resp = await client.put(f"{APP_URL}/api/settings", json=body, timeout=5.0)
    return JSONResponse(content=resp.json(), status_code=resp.status_code)


# ── Image / Audio serving ────────────────────────────────────────────────────

def _resolve_upload_path(path: str) -> Path:
    """Resolve a path that may be absolute (/uploads/...) or relative."""
    base = Path("/uploads").resolve()
    # Paths stored in DB are absolute; accept them directly to avoid double-prefix
    candidate = Path(path).resolve() if path.startswith("/") else (base / path).resolve()
    if not str(candidate).startswith(str(base) + "/") and candidate != base:
        raise HTTPException(status_code=400, detail="Invalid path")
    return candidate


@app.get("/api/audio")
async def serve_audio(path: str):
    full_path = _resolve_upload_path(path)
    if not full_path.exists():
        raise HTTPException(status_code=404, detail="Audio not found")
    return FileResponse(str(full_path))


@app.get("/api/image")
async def serve_image(path: str):
    full_path = _resolve_upload_path(path)
    if not full_path.exists():
        raise HTTPException(status_code=404, detail="Image not found")
    return FileResponse(str(full_path))


# ── Log streaming (SSE) ──────────────────────────────────────────────────────

@app.get("/api/logs/stream")
async def stream_logs(service: str):
    if service == "ollama":
        return StreamingResponse(
            _stream_ollama_log(),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    container_name = CONTAINER_MAP.get(service)
    if not container_name:
        raise HTTPException(status_code=400, detail=f"Unknown service: {service}")

    async def event_generator():
        try:
            docker_client = docker.DockerClient(base_url="unix:///var/run/docker.sock")
        except Exception as e:
            yield f"data: [monitor] Cannot connect to Docker: {e}\n\n"
            return

        try:
            container = docker_client.containers.get(container_name)
        except docker.errors.NotFound:
            yield f"data: [monitor] Container '{container_name}' not found\n\n"
            docker_client.close()
            return

        log_stream = container.logs(stream=True, follow=True, tail=200)
        loop = asyncio.get_event_loop()

        def next_line():
            try:
                return next(log_stream)
            except StopIteration:
                return None

        try:
            while True:
                line = await loop.run_in_executor(None, next_line)
                if line is None:
                    break
                text = line.decode("utf-8", errors="replace").rstrip("\n")
                yield f"data: {text}\n\n"
        finally:
            docker_client.close()

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


async def _stream_ollama_log():
    if not OLLAMA_LOG_FILE.exists():
        yield f"data: [monitor] Log-Datei nicht gefunden: {OLLAMA_LOG_FILE} — Ollama-Service neu starten um Logging zu aktivieren.\n\n"
        return

    with OLLAMA_LOG_FILE.open(errors="replace") as f:
        lines = f.read().splitlines()
    for line in lines[-300:]:
        if line.strip():
            yield f"data: {line}\n\n"

    pos = OLLAMA_LOG_FILE.stat().st_size
    while True:
        await asyncio.sleep(0.5)
        try:
            new_size = OLLAMA_LOG_FILE.stat().st_size
            if new_size > pos:
                with OLLAMA_LOG_FILE.open(errors="replace") as f:
                    f.seek(pos)
                    new_content = f.read()
                pos = new_size
                for line in new_content.splitlines():
                    if line.strip():
                        yield f"data: {line}\n\n"
            elif new_size < pos:
                pos = 0
        except Exception as e:
            yield f"data: [monitor] Lesefehler: {e}\n\n"
            await asyncio.sleep(5)


# ── GPU-Diagnose ─────────────────────────────────────────────────────────────

_DIAG_KEYS = [
    "starting runner", "gpu memory", "loading model", "flash attention",
    "offload", "offloaded", "model weights", "kv cache", "compute graph",
    "total memory", "llama runner started", "llama runner terminated",
    "load_backend", "ggml_cuda_init", "found.*cuda device",
]
_DIAG_RE = re.compile("|".join(_DIAG_KEYS), re.IGNORECASE)


@app.get("/api/gpu-diagnosis")
async def gpu_diagnosis():
    if not OLLAMA_LOG_FILE.exists():
        return {"available": False, "events": [], "parsed": {}}

    lines = OLLAMA_LOG_FILE.read_text(errors="replace").splitlines()

    # Collect last load session (backwards from end)
    session: list[str] = []
    for line in reversed(lines[-2000:]):
        if _DIAG_RE.search(line):
            session.insert(0, line)
        if re.search(r"starting runner", line, re.IGNORECASE) and session:
            break

    parsed: dict = {}
    for line in lines[-2000:]:
        def _extract(pattern: str) -> str | None:
            m = re.search(pattern, line)
            return m.group(1) if m else None

        if "gpu memory" in line and "available=" in line:
            parsed["gpu_available"] = _extract(r'available="([^"]+)"')
            parsed["gpu_free"] = _extract(r'free="([^"]+)"')
        if re.search(r'msg="model weights"', line):
            parsed["weights_device"] = _extract(r'device=(\S+)')
            parsed["weights_size"] = _extract(r'size="([^"]+)"')
        if re.search(r'msg="kv cache"', line):
            parsed["kvcache_device"] = _extract(r'device=(\S+)')
            parsed["kvcache_size"] = _extract(r'size="([^"]+)"')
        if re.search(r'msg="compute graph"', line):
            parsed["compute_device"] = _extract(r'device=(\S+)')
            parsed["compute_size"] = _extract(r'size="([^"]+)"')
        if re.search(r'msg="total memory"', line):
            parsed["total_required"] = _extract(r'size="([^"]+)"')
        if re.search(r'offloaded \d+/\d+ layers', line):
            parsed["layers_offloaded"] = _extract(r'offloaded (\d+/\d+ layers to \S+)')
        if re.search(r'loading model.*requested=', line):
            parsed["layers_requested"] = _extract(r'requested=(\S+)')

    return {"available": True, "events": session[-50:], "parsed": parsed}


