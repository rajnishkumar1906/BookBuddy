from typing import List, Dict
from llm.gemini_client import ask_gemini


def build_books_context(books: List[Dict]) -> str:
    return "\n".join(
        f"{i}. {b['title']} | {b['author']} | {b.get('genres', '')} | {(b.get('description') or '')[:200]}"
        for i, b in enumerate(books, 1)
    )


def librarian_answer(user_question: str, books: List[Dict]) -> str:
    context = build_books_context(books)
    prompt = f"""Answer using only these books. If you can't, say so.

BOOKS:
{context}

Q: {user_question}
A:"""

    return ask_gemini(prompt)
