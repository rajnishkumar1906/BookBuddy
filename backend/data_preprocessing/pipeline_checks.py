import os
import psycopg # type: ignore
import chromadb
from dotenv import load_dotenv
from chromadb.config import Settings

load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")
CLEAN_CSV_PATH = "backend/data/books_clean.csv"
CHROMA_DIR = "backend/chroma_store"


# ---------- CSV CHECK ----------
def cleaned_csv_ready() -> bool:
    return os.path.exists(CLEAN_CSV_PATH) and os.path.getsize(CLEAN_CSV_PATH) > 0


# ---------- SUPABASE CHECK ----------
def supabase_has_books() -> bool:
    if not DATABASE_URL:
        return False

    with psycopg.connect(DATABASE_URL) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM books;")
            count = cur.fetchone()[0]

    return count > 0


# ---------- CHROMA CHECK ----------
def chroma_has_embeddings(collection_name: str = "books") -> bool:
    if not os.path.exists(CHROMA_DIR):
        return False

    client = chromadb.Client(
        Settings(persist_directory=CHROMA_DIR)
    )

    try:
        collection = client.get_collection(collection_name)
        return collection.count() > 0
    except Exception:
        return False
