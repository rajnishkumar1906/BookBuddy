from fastapi import APIRouter
from retrieval.retriever import search_books
from retrieval.supabase_fetch import fetch_books_by_ids
from assistant.librarian import librarian_answer

router = APIRouter()

@router.post("/ask")
def ask(question: str):
    results = search_books(question, top_k=5)
    ids = [r["book_id"] for r in results]
    books = fetch_books_by_ids(ids)

    answer = librarian_answer(question, books)

    return {
        "question": question,
        "answer": answer,
        "sources": books,
    }