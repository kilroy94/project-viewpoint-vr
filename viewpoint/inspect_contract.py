"""Offline, fail-closed binary anchors. Does not load Java classes."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess

HERE = Path(__file__).resolve().parent


def pinned_copy(source, destination, accepted):
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    if digest not in accepted:
        raise ValueError(f"Unsupported {destination.name}: SHA-256 {digest}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    if source.resolve() != destination.resolve():
        shutil.copyfile(source, destination)
    if hashlib.sha256(destination.read_bytes()).hexdigest() != digest:
        raise ValueError("Input changed while copying")
    return digest


def method_block(disassembly, declaration, descriptor):
    blocks = re.split(r"(?=^  \S)", disassembly, flags=re.MULTILINE)
    matches = [b for b in blocks if b.startswith("  " + declaration + "\n")]
    if len(matches) != 1 or f"    descriptor: {descriptor}\n" not in matches[0]:
        raise ValueError(f"Missing/ambiguous method contract: {declaration} {descriptor}")
    return matches[0]


def ordered_calls(block, expected):
    calls = re.findall(r"// (?:InterfaceMethod|Method) (\S+)", block)
    positions = []
    for call in expected:
        if calls.count(call) != 1:
            raise ValueError(f"Expected exactly one invocation of {call}, got {calls.count(call)}")
        positions.append(calls.index(call))
    if positions != sorted(positions):
        raise ValueError("Invocation order changed")
    return len(expected)


CONTRACTS = [
    ("viewpoint.SceneDrawer", "private void drawFrame();", "()V", [
        "viewpoint/render/Retirement.collect:()V",
        "viewpoint/render/WorldRenderer.begin:(Lviewpoint/render/SceneData;Lorg/joml/Matrix4f;FFF)Z",
        "viewpoint/models/Models.release:(Lviewpoint/core/Frame;)V",
        "viewpoint/render/WorldRenderer.finish:(Lviewpoint/render/SceneData;)V",
        "viewpoint/render/Retirement.drawn:(J)V"]),
    ("viewpoint.SceneDrawer", "public void postRender();", "()V", [
        "viewpoint/models/Characters.release:(Lviewpoint/core/Frame;)V",
        "viewpoint/models/Models.release:(Lviewpoint/core/Frame;)V"]),
    ("viewpoint.render.WorldRenderer", "public static boolean begin(viewpoint.render.SceneData, org.joml.Matrix4f, float, float, float);",
     "(Lviewpoint/render/SceneData;Lorg/joml/Matrix4f;FFF)Z", [
        "viewpoint/render/FrameStream.frameStarted:()V",
        "viewpoint/render/TemporalPass.begin:(Lviewpoint/render/FrameContext;)V",
        "viewpoint/render/FrameUniforms.set:(Lviewpoint/render/FrameContext;Lviewpoint/render/Targets;)V",
        "viewpoint/render/Meshes.prepare:(Lviewpoint/render/SceneData;)V",
        "viewpoint/render/ModelPass.prepare:(Lviewpoint/render/SceneData;)V", "drawWorld:()V"]),
    ("viewpoint.render.WorldRenderer", "private static void drawWorld();", "()V", [
        "viewpoint/render/FarPass.prepare:(Lviewpoint/render/FrameContext;)V",
        "viewpoint/render/ShadowPass.draw:(Lviewpoint/render/FrameContext;)V",
        "viewpoint/render/ModelPass.gbuffer:(Lviewpoint/render/FrameContext;)V",
        "viewpoint/render/FarPass.gbuffer:(Lviewpoint/render/FrameContext;)V"]),
    ("viewpoint.render.WorldRenderer", "public static void finish(viewpoint.render.SceneData);",
     "(Lviewpoint/render/SceneData;)V", [
        "viewpoint/render/TemporalPass.resolve:(Lviewpoint/render/FrameContext;)I",
        "viewpoint/render/TemporalPass.remember:(Lviewpoint/render/FrameContext;)V",
        "viewpoint/render/TemporalPass.present:(Lviewpoint/render/FrameContext;IF)V",
        "viewpoint/render/ModelPass.endFrame:()V"]),
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", required=True, type=Path)
    for name in ("game", "viewpoint", "zombiebuddy"):
        parser.add_argument("--" + name, required=True, type=Path)
    args = parser.parse_args()
    build = HERE / "build"
    build.mkdir(exist_ok=True)
    report = build / "contract-report.json"
    # A failed run must never leave an earlier success report behind.
    report.write_text(json.dumps({"status": "incomplete"}) + "\n", encoding="utf-8")
    try:
        pins = json.loads((HERE / "pins.json").read_text(encoding="utf-8"))
        hashes = {name: pinned_copy(getattr(args, name), build / "inputs" / (name + ".jar"), pin["sha256"])
                  for name, pin in pins.items()}
        disassemblies = {}
        for name, _, _, _ in CONTRACTS:
            if name in disassemblies:
                continue
            result = subprocess.run([str(args.java_home / "bin" / "javap.exe"), "-p", "-s", "-c",
                                     "-classpath", str(build / "inputs" / "viewpoint.jar"), name],
                                    check=True, capture_output=True, text=True, encoding="utf-8")
            disassemblies[name] = result.stdout
            (build / (name + ".javap.txt")).write_text(result.stdout, encoding="utf-8")
        anchors = 0
        for name, method, descriptor, calls in CONTRACTS:
            anchors += ordered_calls(method_block(disassemblies[name], method, descriptor), calls)
        result = {"status": "passed", "sha256": hashes, "methods": len(CONTRACTS),
                  "invocation_anchors": anchors, "runtime_validated": False}
        report.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        print(f"Binary contract: {len(CONTRACTS)} methods, {anchors} invocation anchors passed")
    except Exception as error:
        report.write_text(json.dumps({"status": "failed", "error": str(error)}) + "\n", encoding="utf-8")
        raise


if __name__ == "__main__":
    main()
