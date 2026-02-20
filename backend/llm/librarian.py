from typing import List, Dict
from llm.ollama_client import ask_ollama


def build_books_context(books: List[Dict]) -> str:
    lines = []
    for i, b in enumerate(books, 1):
        lines.append(
            f"{i}. Title: {b['title']}\n"
            f"   Author: {b['author']}\n"
            f"   Genres: {b['genres']}\n"
            f"   Pages: {b['num_pages']}\n"
            f"   Description: {b['description'][:300]}...\n"
        )
    return "\n".join(lines)


def librarian_answer(user_question: str, books: List[Dict]) -> str:
    context = build_books_context(books)

    prompt = f"""
You are a helpful librarian.

Below are books retrieved based on the user's interest.
Use ONLY this information to answer.
If the answer is not possible from the books, say so honestly.

BOOKS:
{context}

USER QUESTION:
{user_question}

ANSWER:
"""

    return ask_ollama(prompt)
