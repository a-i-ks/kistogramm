#!/usr/bin/env python3
"""
Benchmark script for AI pipeline timing.
Simulates backend image compression (max 1920x1080, JPEG quality 85%)
and measures Ollama VLM response time per job type.
"""
import base64
import io
import json
import time
import statistics
import httpx
from PIL import Image, ImageDraw, ImageFont

OLLAMA_HOST = "http://localhost:11434"
VLM_MODEL = "qwen2.5vl:7b"

# Backend compression settings (AppSettingsEntity defaults)
MAX_WIDTH = 1920
MAX_HEIGHT = 1080
JPEG_QUALITY = 85

PROMPTS = {
    "INGESTION": """\
You are an inventory assistant. The user described this item as: "Gerät auf dem Tisch"

Look at the image carefully and return a single valid JSON object with these fields:
- "name": the specific name of the object you see
- "description": 1-2 sentences describing color, shape, material, and condition
- "category": exactly one of: Elektronik, Kleidung, Moebelstueck, Lebensmittel, Pflanze, Sonstiges
- "tags": list of 3-5 relevant lowercase tags
- "quantity": number of items visible (integer)
- "purchase_price": null

Respond with ONLY the JSON object, no markdown, no explanation.""",

    "DIMENSION_ESTIMATION": """\
You are an inventory assistant. Look at this item carefully and estimate its physical dimensions.
Return ONLY a JSON object with these fields:
- "width": estimated width as a number
- "widthUnit": unit string (e.g. "cm")
- "height": estimated height as a number
- "heightUnit": unit string
- "depth": estimated depth as a number
- "depthUnit": unit string
- "weight": estimated weight as a number
- "weightUnit": unit string

Use centimeters for dimensions and kg/g for weight. Respond with ONLY the JSON object, no markdown, no explanation.""",

    "VALUE_ESTIMATION": """\
You are an inventory valuation assistant. Look at this item and estimate its current market value.
Return ONLY a JSON object with these fields:
- "minValue": conservative market value as a number (EUR)
- "maxValue": optimistic market value as a number (EUR)
- "currency": "EUR"
- "confidence": one of "low", "medium", "high"
- "reasoning": one sentence explaining the estimate

Respond with ONLY the JSON object, no markdown, no explanation.""",

    "CONDITION_ASSESSMENT": """\
You are an inventory condition assessor. Examine this item carefully.
Return ONLY a JSON object with these fields:
- "condition": one of "neuwertig", "sehr gut", "gut", "akzeptabel", "schlecht"
- "conditionDetails": 1-2 sentences describing visible wear, damage, or notable features

Respond with ONLY the JSON object, no markdown, no explanation.""",

    "TAG_SUGGESTIONS": """\
You are an inventory tagging assistant. Look at this item and suggest relevant tags.
Return ONLY a JSON object with:
- "tags": list of 3-6 relevant lowercase German tags

Respond with ONLY the JSON object, no markdown, no explanation.""",
}


def create_test_image_raw(width: int = 4032, height: int = 3024) -> bytes:
    """Creates a synthetic photo-like JPEG at mobile camera resolution."""
    img = Image.new("RGB", (width, height), color=(180, 160, 140))
    draw = ImageDraw.Draw(img)

    # Background gradient simulation
    for y in range(height):
        shade = int(120 + 60 * (y / height))
        draw.line([(0, y), (width, y)], fill=(shade, shade - 10, shade - 20))

    # Draw a "shelf" surface
    shelf_y = height * 2 // 3
    draw.rectangle([(0, shelf_y), (width, height)], fill=(139, 90, 43))

    # Draw an "item" — a box/device shape
    box_x = width // 3
    box_y = shelf_y - height // 4
    box_w = width // 3
    box_h = height // 4
    draw.rectangle(
        [(box_x, box_y), (box_x + box_w, shelf_y)],
        fill=(50, 80, 120),
        outline=(30, 50, 90),
        width=8,
    )
    # Label on item
    draw.rectangle(
        [(box_x + 20, box_y + 20), (box_x + box_w - 20, box_y + 60)],
        fill=(220, 220, 220),
    )

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=95)
    return buf.getvalue()


def compress_image(raw: bytes) -> bytes:
    """Simulates backend ImageCompressionService: resize to max 1920x1080, quality 85."""
    img = Image.open(io.BytesIO(raw))

    needs_resize = img.width > MAX_WIDTH or img.height > MAX_HEIGHT
    if needs_resize:
        img.thumbnail((MAX_WIDTH, MAX_HEIGHT), Image.LANCZOS)

    buf = io.BytesIO()
    img.save(buf, format="JPEG", quality=JPEG_QUALITY)
    compressed = buf.getvalue()

    # Only use if smaller than original (mirrors backend logic)
    return compressed if len(compressed) < len(raw) else raw


def image_to_b64(data: bytes) -> str:
    return base64.b64encode(data).decode()


def run_single(img_b64: str, job_type: str, prompt: str) -> float:
    payload = {
        "model": VLM_MODEL,
        "prompt": prompt,
        "images": [img_b64],
        "stream": False,
    }
    t0 = time.perf_counter()
    resp = httpx.post(f"{OLLAMA_HOST}/api/generate", json=payload, timeout=600)
    elapsed = time.perf_counter() - t0
    resp.raise_for_status()
    raw = resp.json()["response"].strip()
    print(f"  [{job_type}] {elapsed:.1f}s — response: {raw[:80]}{'...' if len(raw) > 80 else ''}")
    return elapsed


def main():
    print("=== Kistogramm AI Pipeline Benchmark ===")
    print(f"Model: {VLM_MODEL}")
    print(f"Image compression: max {MAX_WIDTH}x{MAX_HEIGHT}, JPEG quality {JPEG_QUALITY}%")
    print()

    print("Generating test image (4032x3024 mobile camera simulation)...")
    raw = create_test_image_raw(4032, 3024)
    print(f"  Raw size: {len(raw)/1024:.0f} KB")

    compressed = compress_image(raw)
    print(f"  Compressed size: {len(compressed)/1024:.0f} KB")
    img_b64 = image_to_b64(compressed)
    print()

    results: dict[str, list[float]] = {jt: [] for jt in PROMPTS}

    # Warm-up run (first Ollama call is often slower due to model loading)
    print("Warm-up run (INGESTION)...")
    warmup_time = run_single(img_b64, "INGESTION_WARMUP", PROMPTS["INGESTION"])
    print(f"  Warm-up: {warmup_time:.1f}s\n")

    # 3 runs per job type
    RUNS = 3
    for run_idx in range(1, RUNS + 1):
        print(f"--- Run {run_idx}/{RUNS} ---")
        for job_type, prompt in PROMPTS.items():
            t = run_single(img_b64, job_type, prompt)
            results[job_type].append(t)
        print()

    print("=== Results ===")
    all_times = []
    for job_type, times in results.items():
        avg = statistics.mean(times)
        mx = max(times)
        mn = min(times)
        all_times.extend(times)
        print(f"  {job_type:<25} avg={avg:.1f}s  min={mn:.1f}s  max={mx:.1f}s")

    overall_avg = statistics.mean(all_times)
    overall_max = max(all_times)
    overall_p95 = sorted(all_times)[int(len(all_times) * 0.95)]

    print()
    print(f"  Overall avg:  {overall_avg:.1f}s")
    print(f"  Overall max:  {overall_max:.1f}s")
    print(f"  Overall p95:  {overall_p95:.1f}s")

    # Recommended timeout: p95 * 1.5, rounded up to next 30s, min 60s
    import math
    recommended = max(60, math.ceil((overall_p95 * 1.5) / 30) * 30)
    print()
    print(f"  Recommended timeout (p95 × 1.5, rounded to 30s): {recommended}s")


if __name__ == "__main__":
    main()
