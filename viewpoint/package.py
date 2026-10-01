"""Package only successfully tested production classes and authored mod assets."""
import hashlib
import io
import json
from pathlib import Path
import re
import zipfile

HERE = Path(__file__).resolve().parent


def main():
    record = json.loads((HERE / "build/latest-run.json").read_text(encoding="utf-8-sig"))
    if record.get("gpuPassed") is not True:
        raise RuntimeError("Packaging requires the standalone GPU test to pass")
    classes = Path(record["classes"]).resolve()
    if not classes.is_relative_to((HERE / "build/runs").resolve()):
        raise RuntimeError("Compiler output must belong to this workspace")
    entries = sorted(classes.rglob("*.class"))
    names = [p.relative_to(classes).as_posix() for p in entries]
    if not names or any(not n.startswith("viewpointvr/") or "Test" in n or "Fixture" in n for n in names):
        raise RuntimeError("Unexpected or test/proprietary class in production output")
    required = ["viewpointvr/diagnostic/Main.class", "viewpointvr/diagnostic/Installation.class",
                "viewpointvr/diagnostic/LwjglGraphics.class", "viewpointvr/FrameBoundary.class",
                "viewpointvr/StageHooks.class", "viewpointvr/instrument/StageTransform.class"]
    if not all(n in names for n in required):
        raise RuntimeError("Incomplete production class set")
    descriptor = (HERE / "mod/42/mod.info").read_text(encoding="utf-8")
    version = re.search(r"^modversion=(.+)$", descriptor, re.MULTILINE)[1].strip()
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w", zipfile.ZIP_DEFLATED) as jar:
        jar.writestr("META-INF/MANIFEST.MF", f"Manifest-Version: 1.0\r\nImplementation-Title: Project Viewpoint VR\r\nImplementation-Version: {version}\r\n\r\n")
        for source, name in zip(entries, names):
            jar.write(source, name)
    jar_bytes = buffer.getvalue()
    jar_hash = hashlib.sha256(jar_bytes).hexdigest()
    dist = HERE / "dist"
    dist.mkdir(exist_ok=True)
    archive = dist / f"ProjectViewpointVR-{version}.zip"
    base = "ProjectViewpointVR/"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as package:
        package.writestr(base + "common/", b"")
        for source in sorted((HERE / "mod").rglob("*")):
            if source.is_file():
                package.write(source, base + source.relative_to(HERE / "mod").as_posix())
        package.writestr(base + "42/media/java/client/ProjectViewpointVR.jar", jar_bytes)
        package.write(HERE / "TESTING.md", base + "README.md")
        package.writestr(base + "SHA256.txt", jar_hash + "  42/media/java/client/ProjectViewpointVR.jar\n")
    with zipfile.ZipFile(archive) as check:
        if check.testzip() is not None:
            raise RuntimeError("Archive verification failed")
        assert base + "42/mod.info" in check.namelist()
        assert base + "42/media/lua/client/ProjectViewpointVR.lua" in check.namelist()
        assert hashlib.sha256(check.read(base + "42/media/java/client/ProjectViewpointVR.jar")).hexdigest() == jar_hash
    digest = hashlib.sha256(archive.read_bytes()).hexdigest()
    archive.with_suffix(".zip.sha256").write_text(digest + "  " + archive.name + "\n", encoding="ascii")
    print(f"Verified manual-test package: {archive}")
    print(f"Production classes: {len(names)}; JAR SHA-256: {jar_hash}")


if __name__ == "__main__":
    main()
