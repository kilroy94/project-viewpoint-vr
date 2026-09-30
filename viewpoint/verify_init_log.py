"""Check the copied-binary test's HotSpot class initialization evidence."""
import re
import sys
from pathlib import Path


def verify(text):
    if "Initializing" not in text or "viewpoint.SceneDrawer" not in text:
        raise ValueError("Missing initialization/target verification evidence")
    forbidden = re.findall(r"^.*Initializing ['\"]?(?:viewpoint[./]|zombie[./]|me[./]zed_0xff[./]zombie_buddy[./]).*$",
                           text, flags=re.MULTILINE)
    if forbidden:
        raise ValueError("Game/mod initialization detected: " + "\n".join(forbidden))


if __name__ == "__main__":
    verify(Path(sys.argv[1]).read_text(encoding="utf-8"))
    print("Initialization audit passed: no Viewpoint, Zomboid or ZombieBuddy classes initialized")
