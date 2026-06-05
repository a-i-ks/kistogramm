import io
import os
import json
import base64
import logging
import re
import time
import datetime
import urllib.parse
import redis
import httpx
from google import genai
from google.genai import types as genai_types
from PIL import Image
from faster_whisper import WhisperModel

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
log = logging.getLogger(__name__)

REDIS_HOST = os.environ.get("REDIS_HOST", "redis")
REDIS_PORT = int(os.environ.get("REDIS_PORT", 6379))
CALLBACK_URL = os.environ["CALLBACK_URL"]
WEBHOOK_SECRET = os.environ["WEBHOOK_SECRET"]
_parsed = urllib.parse.urlparse(CALLBACK_URL)
APP_URL = f"{_parsed.scheme}://{_parsed.netloc}"
OLLAMA_HOST = os.environ.get("OLLAMA_HOST", "http://host.docker.internal:11434")
QUEUE_KEY = "ai_jobs_queue"
VLM_MODEL = os.environ.get("VLM_MODEL", "qwen2.5vl:7b")

PROMPT_INGESTION_BASE = """\
You are an inventory assistant. Your task is to identify the object in the image as precisely as possible.
{context_block}
Look at the image carefully and return a single valid JSON object with these fields:
- "name": the specific name of the object in German, as precise as possible (e.g. "blaue Sprühflasche", "Holzstuhl mit Armlehnen"). Use the user's input above to make the name more accurate.
- "description": 1-2 sentences in German describing color, shape, material, and condition. Incorporate any details from the user's input.
- "category": exactly one of: Elektronik, Kleidung, Moebelstueck, Lebensmittel, Pflanze, Sonstiges
- "quantity": number of items visible (integer)
- "purchase_price": null

All text values must be in German. Respond with ONLY the JSON object, no markdown, no explanation."""


def build_ingestion_prompt(transcript: str, context_hint: str) -> str:
    parts = []
    if transcript:
        parts.append(f'The user described this item by voice: "{transcript}"')
    if context_hint:
        parts.append(f'Additional user-provided info: "{context_hint}"')
    if parts:
        context_block = "The user has already provided the following information about this item — use it to improve your identification:\n" + "\n".join(parts) + "\n"
    else:
        context_block = ""
    return PROMPT_INGESTION_BASE.format(context_block=context_block)

PROMPT_DIMENSION = """\
You are an inventory assistant. Look at this item carefully and estimate its physical dimensions.
Return ONLY a JSON object with these fields:
- "width": estimated width as a number (e.g. 30.5)
- "widthUnit": unit string (e.g. "cm")
- "height": estimated height as a number
- "heightUnit": unit string
- "depth": estimated depth as a number
- "depthUnit": unit string
- "weight": estimated weight as a number
- "weightUnit": unit string (e.g. "kg" or "g")

Use centimeters for dimensions and kg/g for weight. Respond with ONLY the JSON object, no markdown, no explanation."""

PROMPT_VALUE = """\
You are an inventory valuation assistant. Look at this item and estimate its current market value.
Return ONLY a JSON object with these fields:
- "minValue": conservative market value as a number (EUR)
- "maxValue": optimistic market value as a number (EUR)
- "currency": "EUR"
- "confidence": one of "low", "medium", "high"
- "reasoning": one sentence in German explaining the estimate

Respond with ONLY the JSON object, no markdown, no explanation."""

PROMPT_CONDITION = """\
You are an inventory condition assessor. Examine this item carefully.
Return ONLY a JSON object with these fields:
- "condition": one of "neuwertig", "sehr gut", "gut", "akzeptabel", "schlecht"
- "conditionDetails": 1-2 sentences in German describing visible wear, damage, or notable features

Respond with ONLY the JSON object, no markdown, no explanation."""

PROMPT_TAGS = """\
You are an inventory tagging assistant. Look at this item and suggest relevant tags.
Return ONLY a JSON object with:
- "tags": list of 3-6 relevant lowercase tags in German (e.g. ["elektronik", "kabel", "usb"])

Respond with ONLY the JSON object, no markdown, no explanation."""

_settings_cache: dict | None = None
_settings_cache_ts: float = 0.0
_SETTINGS_TTL = 30.0


def _get_vlm_config() -> dict:
    global _settings_cache, _settings_cache_ts
    now = time.time()
    if _settings_cache is not None and (now - _settings_cache_ts) < _SETTINGS_TTL:
        return _settings_cache
    try:
        resp = httpx.get(f"{APP_URL}/api/settings", timeout=5)
        resp.raise_for_status()
        _settings_cache = resp.json()
        _settings_cache_ts = now
        log.info("Settings: provider=%s model=%s device=%s",
                 _settings_cache.get("vlmProvider", "ollama"),
                 _settings_cache.get("vlmModel"), _settings_cache.get("vlmDevice"))
    except Exception as e:
        log.warning("Cannot fetch settings: %s — using cache/defaults", e)
        if _settings_cache is None:
            _settings_cache = {"vlmProvider": "ollama", "vlmModel": VLM_MODEL,
                               "vlmDevice": "auto", "vlmNumCtx": 4096, "vlmNumThread": 4}
    return _settings_cache


def _encode_image_for_vlm(image_path: str) -> str:
    cfg = _get_vlm_config()
    if not cfg.get("vlmImageCompressionEnabled", True):
        with open(image_path, "rb") as f:
            return base64.b64encode(f.read()).decode()

    max_w = cfg.get("vlmImageMaxWidth", 672)
    max_h = cfg.get("vlmImageMaxHeight", 448)
    quality = cfg.get("vlmImageQuality", 85)

    img = Image.open(image_path).convert("RGB")
    orig_w, orig_h = img.size
    img.thumbnail((max_w, max_h), Image.LANCZOS)
    new_w, new_h = img.size

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=quality)
    encoded = base64.b64encode(buf.getvalue()).decode()
    log.info("VLM image: %dx%d → %dx%d (%.1f KB)", orig_w, orig_h, new_w, new_h, len(buf.getvalue()) / 1024)
    return encoded


def check_ollama_health() -> bool:
    try:
        resp = httpx.get(f"{OLLAMA_HOST}/api/tags", timeout=5)
        resp.raise_for_status()
        return True
    except Exception as exc:
        log.error("OLLAMA_UNREACHABLE: Ollama is not reachable at %s — %s", OLLAMA_HOST, exc)
        return False


log.info("Loading Whisper model (CPU)...")
whisper_model = WhisperModel("base", device="cpu", compute_type="int8")
log.info("Whisper model loaded.")

cfg_init = _get_vlm_config()
if cfg_init.get("vlmProvider", "ollama") == "ollama":
    if not check_ollama_health():
        log.warning("OLLAMA_UNREACHABLE: Starting worker anyway, but AI jobs will fail until Ollama is available")


def transcribe(audio_path: str) -> str:
    log.info("Transcribing audio: %s", audio_path)
    t0 = time.time()
    segments, _ = whisper_model.transcribe(audio_path, beam_size=5, language="de")
    result = " ".join(s.text for s in segments).strip()
    log.info("Transcription done in %.1fs: '%s'", time.time() - t0, result[:120])
    return result


# ── External API helpers ───────────────────────────────────────────────────────

def _call_openai(image_b64: str, prompt: str, api_key: str) -> str:
    log.info("Calling OpenAI GPT-4o-mini")
    t0 = time.time()
    payload = {
        "model": "gpt-4o-mini",
        "messages": [{"role": "user", "content": [
            {"type": "text", "text": prompt},
            {"type": "image_url", "image_url": {"url": f"data:image/jpeg;base64,{image_b64}"}},
        ]}],
        "max_tokens": 512,
    }
    resp = httpx.post(
        "https://api.openai.com/v1/chat/completions",
        json=payload,
        headers={"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"},
        timeout=60,
    )
    resp.raise_for_status()
    raw = resp.json()["choices"][0]["message"]["content"].strip()
    log.info("OpenAI response in %.1fs: %s", time.time() - t0, raw[:200])
    return raw


def _call_gemini(image_b64: str, prompt: str, api_key: str) -> str:
    log.info("Calling Google Gemini 2.0 Flash Lite (SDK)")
    t0 = time.time()
    client = genai.Client(api_key=api_key)
    response = client.models.generate_content(
        model="gemini-3.5-flash",
        contents=[
            genai_types.Part.from_text(text=prompt),
            genai_types.Part(inline_data=genai_types.Blob(
                mime_type="image/jpeg",
                data=base64.b64decode(image_b64),
            )),
        ],
        config=genai_types.GenerateContentConfig(
            response_mime_type="application/json",
            max_output_tokens=512,
        ),
    )
    raw = response.text.strip()
    log.info("Gemini response in %.1fs: %s", time.time() - t0, raw[:200])
    return raw


def _call_ollama(image_b64: str, prompt: str, cfg: dict, keep_alive=None) -> str:
    model = cfg.get("vlmModel", VLM_MODEL)
    options: dict = {"num_ctx": cfg.get("vlmNumCtx", 4096)}
    device = cfg.get("vlmDevice", "auto")
    if device == "cpu":
        options["num_gpu"] = 0
        options["num_thread"] = cfg.get("vlmNumThread", 4)
    elif device == "gpu":
        options["num_gpu"] = 9999

    payload = {"model": model, "prompt": prompt, "images": [image_b64], "stream": False, "options": options}
    if keep_alive is not None:
        payload["keep_alive"] = keep_alive
    log.info("Calling Ollama (%s) device=%s keep_alive=%s", model, device, keep_alive)
    t0 = time.time()
    resp = httpx.post(f"{OLLAMA_HOST}/api/generate", json=payload, timeout=600)
    resp.raise_for_status()
    raw = resp.json()["response"].strip()
    log.info("Ollama response in %.1fs: %s", time.time() - t0, raw[:200])
    return raw


# ── Gemini rate-limit tracking (Redis-backed) ─────────────────────────────────

GEMINI_RPM_LIMIT = 30
GEMINI_RPD_LIMIT = 1500


def _gemini_check_and_increment(r) -> None:
    minute_key = f"gemini:rpm:{int(time.time() // 60)}"
    day_key = f"gemini:rpd:{datetime.date.today().isoformat()}"

    rpm = int(r.get(minute_key) or 0)
    rpd = int(r.get(day_key) or 0)

    if rpm >= GEMINI_RPM_LIMIT:
        raise RuntimeError(f"GEMINI_RATE_LIMIT: {rpm}/{GEMINI_RPM_LIMIT} requests this minute — try again in a moment")
    if rpd >= GEMINI_RPD_LIMIT:
        raise RuntimeError(f"GEMINI_RATE_LIMIT: {rpd}/{GEMINI_RPD_LIMIT} requests today — daily limit reached")

    pipe = r.pipeline()
    pipe.incr(minute_key)
    pipe.expire(minute_key, 120)
    pipe.incr(day_key)
    pipe.expire(day_key, 172800)
    pipe.execute()
    log.info("Gemini rate: %d+1/%d rpm, %d+1/%d rpd", rpm, GEMINI_RPM_LIMIT, rpd, GEMINI_RPD_LIMIT)


def _openai_increment(r) -> None:
    day_key = f"openai:rpd:{datetime.date.today().isoformat()}"
    pipe = r.pipeline()
    pipe.incr(day_key)
    pipe.expire(day_key, 172800)
    pipe.execute()


# ── Core VLM call (routes to provider) ────────────────────────────────────────

def _vlm_call(image_path: str, prompt: str, r, keep_alive=None) -> str:
    img_b64 = _encode_image_for_vlm(image_path)
    cfg = _get_vlm_config()
    provider = cfg.get("vlmProvider", "ollama")

    if provider == "openai":
        api_key = cfg.get("openaiApiKey") or ""
        if not api_key:
            raise RuntimeError("OPENAI_KEY_MISSING: No OpenAI API key configured in settings")
        _openai_increment(r)
        return _call_openai(img_b64, prompt, api_key)
    elif provider == "gemini":
        api_key = cfg.get("geminiApiKey") or ""
        if not api_key:
            raise RuntimeError("GEMINI_KEY_MISSING: No Gemini API key configured in settings")
        _gemini_check_and_increment(r)
        return _call_gemini(img_b64, prompt, api_key)
    else:
        return _call_ollama(img_b64, prompt, cfg, keep_alive)


def analyze_with_vlm(image_path: str, transcript: str, context_hint: str = "", r=None, keep_alive=None) -> dict:
    prompt = build_ingestion_prompt(transcript, context_hint)
    raw = _vlm_call(image_path, prompt, r, keep_alive=keep_alive)
    result = _parse_json_response(raw)
    result.setdefault("name", transcript)
    result.setdefault("description", "")
    result.setdefault("category", "Sonstiges")
    result.setdefault("quantity", 1)
    result.setdefault("purchase_price", None)
    return result


def analyze_with_prompt(image_path: str, prompt: str, r=None, keep_alive=None) -> dict:
    raw = _vlm_call(image_path, prompt, r, keep_alive=keep_alive)
    return _parse_json_response(raw)


def _parse_json_response(raw: str) -> dict:
    raw = re.sub(r"^```(?:json)?\s*", "", raw)
    raw = re.sub(r"\s*```$", "", raw)
    start = raw.index("{")
    end = raw.rindex("}") + 1
    return json.loads(raw[start:end])


def send_callback(job_id: str, job_type: str, result: dict = None, proposal_data: str = None, error: str = None, transcript: str = None) -> None:
    if error is not None:
        payload = {"jobId": job_id, "jobType": job_type, "error": error}
    elif job_type == "INGESTION":
        payload = {"jobId": job_id, "jobType": job_type, **(result or {})}
        if "purchase_price" in payload:
            payload["purchasePrice"] = payload.pop("purchase_price")
        if transcript:
            payload["transcript"] = transcript
    else:
        payload = {"jobId": job_id, "jobType": job_type, "proposalData": proposal_data}

    headers = {"X-Webhook-Secret": WEBHOOK_SECRET, "Content-Type": "application/json"}
    resp = httpx.post(CALLBACK_URL, json=payload, headers=headers, timeout=30)
    resp.raise_for_status()
    log.info("Callback sent for jobId=%s type=%s → HTTP %d", job_id, job_type, resp.status_code)


ANALYSIS_PROMPTS = {
    "DIMENSION_ESTIMATION": PROMPT_DIMENSION,
    "VALUE_ESTIMATION": PROMPT_VALUE,
    "CONDITION_ASSESSMENT": PROMPT_CONDITION,
    "TAG_SUGGESTIONS": PROMPT_TAGS,
}


def notify_started(job_id: str) -> None:
    try:
        headers = {"X-Webhook-Secret": WEBHOOK_SECRET}
        httpx.post(f"{APP_URL}/api/ai/jobs/{job_id}/start", headers=headers, timeout=5)
    except Exception as exc:
        log.warning("Could not notify start for job %s: %s", job_id, exc)


def process_job(job: dict, r) -> None:
    job_id = job["jobId"]
    job_type = job.get("jobType", "INGESTION")
    log.info("Processing job %s type=%s", job_id, job_type)

    notify_started(job_id)

    cfg = _get_vlm_config()
    provider = cfg.get("vlmProvider", "ollama")

    if provider == "ollama" and not check_ollama_health():
        raise RuntimeError(f"OLLAMA_UNREACHABLE: Ollama not available at {OLLAMA_HOST}, cannot process job {job_id}")

    queue_depth = r.llen(QUEUE_KEY)
    keep_alive = -1 if queue_depth > 0 else None
    log.info("Provider=%s queue_depth=%d keep_alive=%s", provider, queue_depth, keep_alive)

    if job_type == "INGESTION":
        audio_path = job.get("audioPath", "")
        transcript = transcribe(audio_path) if audio_path else ""
        log.info("Transcript for job %s: %s", job_id, transcript[:120])

        context_hint = job.get("contextHint", "")
        result = analyze_with_vlm(job["imagePath"], transcript, context_hint,
                                  r=r, keep_alive=keep_alive)
        log.info("VLM result for job %s: %s", job_id, result)

        send_callback(job_id, job_type="INGESTION", result=result, transcript=transcript)

    elif job_type in ANALYSIS_PROMPTS:
        prompt = ANALYSIS_PROMPTS[job_type]
        proposal = analyze_with_prompt(job["imagePath"], prompt, r=r, keep_alive=keep_alive)
        log.info("Proposal for job %s: %s", job_id, proposal)

        send_callback(job_id, job_type=job_type, proposal_data=json.dumps(proposal))

    else:
        log.warning("Unknown job type '%s' for job %s — skipping", job_type, job_id)
        return

    log.info("Job %s completed successfully", job_id)


def main() -> None:
    r = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, decode_responses=True, socket_timeout=None)
    log.info("Worker ready, listening on queue '%s'...", QUEUE_KEY)

    while True:
        job = None
        raw = None
        try:
            item = r.blpop(QUEUE_KEY, timeout=30)
            if item is None:
                continue
            _, raw = item
            job = json.loads(raw)

            # Check if job was paused while waiting in the queue
            try:
                resp = httpx.get(f"{APP_URL}/api/ai/jobs/{job['jobId']}", timeout=5)
                if resp.status_code == 200 and resp.json().get("status") == "PAUSED":
                    log.info("Job %s is PAUSED, re-queueing", job["jobId"])
                    r.rpush(QUEUE_KEY, raw)
                    time.sleep(5)
                    continue
            except Exception as check_exc:
                log.warning("Could not check job status for %s, proceeding: %s", job.get("jobId"), check_exc)

            process_job(job, r)
        except Exception as exc:
            log.error("Job failed: %s", exc, exc_info=True)
            if job is not None:
                try:
                    send_callback(job["jobId"], job.get("jobType", "INGESTION"), error=str(exc))
                except Exception as cb_exc:
                    log.error("Failed to send error callback for job %s: %s", job.get("jobId"), cb_exc)


if __name__ == "__main__":
    main()
