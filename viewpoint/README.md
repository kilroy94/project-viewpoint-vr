# Viewpoint development baseline 0.2.0

This is an **offline development baseline**, not an installable VR mod. It checks the exact binary contract, tests camera math and a synchronous two-eye lifecycle, and verifies a dormant entry hook against copied Viewpoint bytecode. No live renderer adapter or headset output is implemented yet.

Requirements: Python 3, JDK 25, and separately obtained pinned Viewpoint 0.1.3 / Project Zomboid 42.21.0 / ZombieBuddy 2.3.2 binaries. No proprietary binaries are included.

```powershell
./viewpoint/Test.ps1 -JavaHome 'path/to/jdk-25' -GameJar 'path/to/projectzomboid.jar' -ViewpointJar 'path/to/Viewpoint.jar' -ZombieBuddyJar 'path/to/ZombieBuddy.jar'
```

The script copies verified inputs into ignored `viewpoint/build/inputs`, runs JDK `javap`, compiles only this project's sources/tests, and writes `viewpoint/build/contract-report.json`. Each run has fresh compiler output under `viewpoint/build/runs/<id>`. The copied-binary test defines/retransforms SceneDrawer without initialization; a checked JVM class-initialization log records that boundary. It does not launch Zomboid, install a mod, or change runtime/user settings. Hash or contract mismatch is a hard failure. Accepted loader hashes include the original 2.3.2 and the previously audited B42.21 temporary fix.

The output `viewpoint-vr-core.jar` contains only this project's production classes, not test fixtures, the test agent, dependencies or proprietary classes. It is a development library, **not something to install in Zomboid**. ASM is supplied on the development classpath by the separately obtained ZombieBuddy JAR. A future loader integration must provide that dependency and establish classloader ownership before enabling any hook.

See [the render boundary](RENDER-BOUNDARY.md) for the entry transform, driver ownership and cleanup contracts. All current driver registrations are in synthetic tests; no production entry point installs or enables the transform.

`StereoCamera` accepts a scene-space center eye and camera-to-scene quaternion, in a right-handed Y-up system looking down local -Z. Synthetic eyes use a configurable IPD in scene units; physical meter-to-scene calibration remains unvalidated. Asymmetric FOV values are signed tangents, not degrees or radians. Its OpenGL projection retains Viewpoint's 0.05/400 clipping planes. Output matrices/vectors are independent, caller-owned values.

`inspect_contract.py` verifies selected method descriptors and lexical invocation order/counts. These are regression anchors for the audited binary, not a control-flow proof, bytecode transformer, or demonstration of runtime safety. See [the integration plan](../docs/VIEWPOINT-PLAN.md) for the remaining renderer work.
