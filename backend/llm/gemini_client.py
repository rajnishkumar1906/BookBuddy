"""
Gemini LLM client. API key from env GEMINI_API_KEY or backend config.
"""
# from google import genai
import google.genai as genai
from core.config import settings

_client = None

def _get_client():
    global _client
    if _client is None:
        api_key = (settings.GEMINI_API_KEY or "").strip() or None
        _client = genai.Client(api_key=api_key) if api_key else genai.Client()
    return _client


# GEMINI_MODEL = "gemini-2.0-flash"  # or "gemini-3-flash-preview"
GEMINI_MODEL = "gemini-1.5-flash"  # or "gemini-3-flash-preview"


def ask_gemini(prompt: str) -> str:
    """Send prompt to Gemini and return response text."""
    api_key = (settings.GEMINI_API_KEY or "").strip()
    if not api_key:
        raise ValueError("GEMINI_API_KEY is not set. Add it to your .env file.")
    client = _get_client()
    response = client.models.generate_content(
        model=GEMINI_MODEL,
        contents=prompt,
    )
    return (response.text or "").strip()
