"""Small educational Python runner.

This is an application sandbox, not a security boundary. It intentionally blocks
common OS/network/process primitives for beginner lessons while permitting the
language itself to be practiced interactively.
"""

import ast
import contextlib
import io
import math
import statistics
import json
import random

SAFE_IMPORTS = {"math", "statistics", "json", "random", "datetime", "itertools", "functools", "collections"}
BLOCKED_NAMES = {
    "__import__", "eval", "exec", "compile", "open", "input", "breakpoint",
    "globals", "locals", "vars", "dir", "help", "exit", "quit",
}
BLOCKED_MODULES = {
    "os", "sys", "subprocess", "socket", "ctypes", "pathlib", "shutil",
    "importlib", "multiprocessing", "threading", "signal", "resource",
}


class SandboxViolation(ValueError):
    pass


def _validate(tree: ast.AST) -> None:
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                root = alias.name.split(".", 1)[0]
                if root in BLOCKED_MODULES or root not in SAFE_IMPORTS:
                    raise SandboxViolation(f"Import '{alias.name}' is disabled in this lesson sandbox.")
        elif isinstance(node, ast.ImportFrom):
            root = (node.module or "").split(".", 1)[0]
            if root in BLOCKED_MODULES or root not in SAFE_IMPORTS:
                raise SandboxViolation(f"Import from '{node.module}' is disabled in this lesson sandbox.")
        elif isinstance(node, ast.Name) and node.id in BLOCKED_NAMES:
            raise SandboxViolation(f"'{node.id}' is disabled in this lesson sandbox.")
        elif isinstance(node, ast.Attribute) and node.attr in BLOCKED_NAMES:
            raise SandboxViolation(f"Attribute '{node.attr}' is disabled in this lesson sandbox.")


def run_user_code(source: str) -> dict:
    tree = ast.parse(source, mode="exec")
    _validate(tree)
    output = io.StringIO()
    scope = {
        "math": math,
        "statistics": statistics,
        "json": json,
        "random": random,
        "__name__": "__pet_lesson__",
    }
    try:
        with contextlib.redirect_stdout(output), contextlib.redirect_stderr(output):
            exec(compile(tree, "<pet-lesson>", "exec"), scope, scope)
        return {"ok": True, "stdout": output.getvalue(), "error": ""}
    except Exception as exc:
        return {"ok": False, "stdout": output.getvalue(), "error": f"{type(exc).__name__}: {exc}"}
