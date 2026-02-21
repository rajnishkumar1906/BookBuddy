from typing import List, Dict
from llm.gemini_client import ask_gemini


def librarian_answer(question: str, books: List[Dict]) -> Dict:
    """
    Returns:
    {
        "answer": str,
        "citations": { "[1]": book_id, ... }
    }
    """

    if not books:
        return {
            "answer": "I don’t have enough information from the available books to answer this question.",
            "citations": {}
        }

    # Build strict context
    context_lines = []
    citation_map = {}

    for i, b in enumerate(books, 1):
        desc = (b.get("description") or "")[:200]
        context_lines.append(f"[{i}] {b['title']} | {b['author']} | {b.get('genres', '')} | {desc}")
        citation_map[f"[{i}]"] = b["book_id"]
    context = "\n".join(context_lines)

    prompt = f"""Answer using ONLY the books below. Cite each claim with [1],[2],etc. If you cannot answer from these books, say exactly:
  "I don’t have enough information from the available books to answer this question."
BOOKS:
{context}

Q: {question}
A:"""

    answer = ask_gemini(prompt).strip()
    print(f'Answer given by gemini : {answer}')
    # Hard hallucination check
    if "enough information" in answer.lower():
        return {
            "answer": "I don’t have enough information from the available books to answer this question.",
            "citations": {}
        }

    # Validate citations
    used_citations = {
        c: citation_map[c]
        for c in citation_map
        if c in answer
    }

    if not used_citations:
        return {
            "answer": "I don’t have enough information from the available books to answer this question.",
            "citations": {}
        }

    return {
        "answer": answer,
        "citations": used_citations
    }