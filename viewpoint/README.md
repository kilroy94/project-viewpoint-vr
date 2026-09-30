# Viewpoint baseline 0.1.0

This is an **offline development baseline**, not an installable VR mod. It checks the exact installed binary contract without loading game or mod classes, and tests camera math for Viewpoint's coordinate system. No renderer interception or headset output is implemented yet.

Requirements: Python 3, JDK 25, and separately obtained pinned Viewpoint 0.1.3 / Project Zomboid 42.21.0 / ZombieBuddy 2.3.2 binaries. No proprietary binaries are included.

```powershell
./viewpoint/Test.ps1 -JavaHome 'path/to/jdk-25' -GameJar 'path/to/projectzomboid.jar' -ViewpointJar 'path/to/Viewpoint.jar' -ZombieBuddyJar 'path/to/ZombieBuddy.jar'
```

The script copies verified inputs into ignored `viewpoint/build/inputs`, runs JDK `javap` against those copies, compiles only this project's camera code/tests, and writes `viewpoint/build/contract-report.json`. It does not initialize Viewpoint, launch Zomboid, install a mod, or change runtime/user settings. Hash or contract mismatch is a hard failure. Accepted loader hashes include the original 2.3.2 and the previously audited B42.21 temporary fix.

`StereoCamera` accepts a scene-space center eye and camera-to-scene quaternion, in a right-handed Y-up system looking down local -Z. Synthetic eyes use a configurable IPD in scene units; physical meter-to-scene calibration remains unvalidated. Asymmetric FOV values are signed tangents, not degrees or radians. Its OpenGL projection retains Viewpoint's 0.05/400 clipping planes. Output matrices/vectors are independent, caller-owned values.

`inspect_contract.py` verifies selected method descriptors and lexical invocation order/counts. These are regression anchors for the audited binary, not a control-flow proof, bytecode transformer, or demonstration of runtime safety. See [the integration plan](../docs/VIEWPOINT-PLAN.md) for the remaining renderer work.
