import csv
import time
import requests
import webbrowser
from pathlib import Path

# -------- CONFIG --------
CSV_PATH = "data/books.csv"          # <-- change this
IMAGE_COLUMN = "cover_image_uri"     # <-- change if needed
MAX_IMAGES = 30
DELAY_SECONDS = 1.5
TIMEOUT = 10
# ------------------------


def is_image_accessible(url: str) -> bool:
    try:
        headers = {"User-Agent": "Mozilla/5.0"}
        r = requests.get(url, headers=headers, timeout=TIMEOUT, stream=True)

        if r.status_code != 200:
            return False

        content_type = r.headers.get("Content-Type", "")
        return content_type.startswith("image/")

    except Exception:
        return False


def open_images(start_row: int, end_row: int):
    csv_path = Path(CSV_PATH)

    if not csv_path.exists():
        print(f"❌ CSV file not found: {CSV_PATH}")
        return

    with open(csv_path, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))

    total_rows = len(rows)

    if start_row < 0 or end_row >= total_rows or start_row > end_row:
        print("❌ Invalid row range")
        return

    count = end_row - start_row + 1
    if count > MAX_IMAGES:
        print(f"❌ Limit is {MAX_IMAGES} images at a time")
        return

    # ---- Register Chrome if needed ----
    try:
        chrome = webbrowser.get("chrome")
    except webbrowser.Error:
        # Uncomment ONE of these if Chrome is not detected

        # WINDOWS
        # webbrowser.register(
        #     "chrome",
        #     None,
        #     webbrowser.BackgroundBrowser(
        #         "C:/Program Files/Google/Chrome/Application/chrome.exe"
        #     ),
        # )

        # MAC
        # webbrowser.register(
        #     "chrome",
        #     None,
        #     webbrowser.BackgroundBrowser(
        #         "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
        #     ),
        # )

        chrome = webbrowser.get()

    print(f"\n🔍 Testing & opening images (rows {start_row} → {end_row})\n")

    for i in range(start_row, end_row + 1):
        url = rows[i].get(IMAGE_COLUMN, "").strip()

        if not url:
            print(f"[Row {i}] ❌ No image URL")
            continue

        print(f"[Row {i}] 🔎 Testing...")
        if is_image_accessible(url):
            print(f"[Row {i}] ✅ Opening in Chrome")
            chrome.open_new_tab(url)
            time.sleep(DELAY_SECONDS)
        else:
            print(f"[Row {i}] ❌ Image not accessible")


if __name__ == "__main__":
    start = int(input("Enter start row: "))
    end = int(input("Enter end row: "))

    open_images(start, end)
