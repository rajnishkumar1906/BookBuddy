import os
from dotenv import load_dotenv
import psycopg
import chromadb
from chromadb.config import Settings
from sentence_transformers import SentenceTransformer

# ---------------- ENV ----------------
load_dotenv()
DATABASE_URL = os.getenv("DATABASE_URL")
# ------------------------------------

# --------- FORCE CPU (SAFE) ----------
os.environ["CUDA_VISIBLE_DEVICES"] = ""

# --------- PATH SETUP ----------------
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHROMA_DIR = os.path.join(BASE_DIR, "chroma_store_test")

os.makedirs(CHROMA_DIR, exist_ok=True)

print("📁 Chroma will persist at:")
print(CHROMA_DIR)
print("📁 Exists before run:", os.path.exists(CHROMA_DIR))


def build_embedding_text(row):
    return (
        f"Title: {row[1]}\n"
        f"Author: {row[2]}\n"
        f"Genres: {row[3]}\n"
        f"Pages: {row[5]}\n"
        f"Description: {row[4]}"
    )


def main():
    print("🔌 Fetching 100 books from Supabase...")
    with psycopg.connect(DATABASE_URL) as conn:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT
                    book_id,
                    book_title,
                    author,
                    genres,
                    book_details,
                    num_pages
                FROM books
                LIMIT 100;
            """)
            rows = cur.fetchall()

    print(f"✅ Fetched {len(rows)} rows")

    print("🧠 Loading embedding model (CPU)...")
    model = SentenceTransformer("all-mpnet-base-v2", device="cpu")

    print("🗂 Initializing Chroma (TEST)...")
    client = chromadb.Client(
        Settings(persist_directory=CHROMA_DIR)
    )
    collection = client.get_or_create_collection("books_test")

    print("⚙️ Creating embeddings (100 rows)...")

    texts = [build_embedding_text(r) for r in rows]
    ids = [str(r[0]) for r in rows]

    embeddings = model.encode(
        texts,
        batch_size=16,
        max_length=256,
        normalize_embeddings=True,
        show_progress_bar=True,
    )

    collection.add(
        ids=ids,
        embeddings=embeddings.tolist(),
    )

    print("💾 Forcing persist...")
    # client.persist()
    client._system.stop()

    print("📁 Exists after persist:", os.path.exists(CHROMA_DIR))
    print("📁 Contents:", os.listdir(CHROMA_DIR))

    print("🔢 Vector count:", collection.count())
    print("✅ TEST COMPLETE")


if __name__ == "__main__":
    main()