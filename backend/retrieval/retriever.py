from sentence_transformers import SentenceTransformer
from retrieval.chroma_client import get_chroma_collection

model = SentenceTransformer("all-mpnet-base-v2", device="cpu")

def search_books(query: str, top_k: int = 5):
    collection = get_chroma_collection()

    embedding = model.encode(
        query,
        normalize_embeddings=True,
    ).tolist()

    results = collection.query(
        query_embeddings=[embedding],
        n_results=top_k,
    )

    return [
        {
            "book_id": results["ids"][0][i],
            "distance": results["distances"][0][i],
        }
        for i in range(len(results["ids"][0]))
    ]