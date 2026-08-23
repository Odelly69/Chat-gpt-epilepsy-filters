import json
from .curriculum import CURRICULUM
from .pet import VirtualPet
from .sandbox import run_user_code

PET = VirtualPet()


def status() -> str:
    return json.dumps(PET.status())


def curriculum() -> str:
    return json.dumps([
        {"id": i, "title": title, "topics": topics}
        for i, title, topics in CURRICULUM
    ])


def action(name: str) -> str:
    if name == "feed":
        return PET.feed()
    if name == "play":
        return PET.play()
    if name.startswith("teach:"):
        return PET.teach(name.split(":", 1)[1])
    return "Unknown pet action."


def execute(source: str) -> str:
    return json.dumps(run_user_code(source))
