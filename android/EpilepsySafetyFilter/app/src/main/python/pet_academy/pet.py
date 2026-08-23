from dataclasses import dataclass, field
from typing import Dict


@dataclass
class VirtualPet:
    name: str = "Pip"
    hunger: int = 20
    energy: int = 80
    happiness: int = 70
    knowledge: int = 0
    friendship: int = 0
    badges: list[str] = field(default_factory=list)
    variables: Dict[str, object] = field(default_factory=dict)

    def feed(self) -> str:
        self.hunger = max(0, self.hunger - 20)
        self.happiness = min(100, self.happiness + 5)
        return f"{self.name} happily eats. Hunger is {self.hunger}%."

    def play(self) -> str:
        self.energy = max(0, self.energy - 10)
        self.happiness = min(100, self.happiness + 10)
        self.friendship = min(100, self.friendship + 2)
        return f"{self.name} plays with you! Friendship is {self.friendship}%."

    def teach(self, lesson_id: str) -> str:
        self.knowledge = min(100, self.knowledge + 5)
        if lesson_id not in self.badges:
            self.badges.append(lesson_id)
        self.happiness = min(100, self.happiness + 3)
        return f"{self.name} learned {lesson_id}. Knowledge is {self.knowledge}%."

    def status(self) -> dict:
        return {
            "name": self.name,
            "hunger": self.hunger,
            "energy": self.energy,
            "happiness": self.happiness,
            "knowledge": self.knowledge,
            "friendship": self.friendship,
            "badges": list(self.badges),
        }
