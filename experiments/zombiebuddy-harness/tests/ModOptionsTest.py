"""Exercise the settings script with vanilla ModOptions.lua and in-memory game services.

Usage: python tests/ModOptionsTest.py <path-to-vanilla-PZAPI/ModOptions.lua>
Requires lupa (may be installed locally in build/lua-test-tools). No game entrypoint runs.
"""
import sys
from pathlib import Path

root = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(root / "build/lua-test-tools"))
from lupa.lua51 import LuaRuntime

lua = LuaRuntime()
lua.execute(r'''
function getText(value) return value end
function require(_) end
Keyboard = {KEY_SCROLL=70, KEY_F10=68}
Events = {}
for _, name in ipairs({"OnMainMenuEnter", "OnGameStart", "OnTickEvenPaused", "OnGamepadDisconnect"}) do
    local event = {handlers={}}
    event.Add = function(fn) table.insert(event.handlers, fn) end
    event.fire = function(...) for _, fn in ipairs(event.handlers) do fn(...) end end
    Events[name] = event
end
local core = {isDoingTextEntry=function() return typing == true end}
function getCore() return core end
MainOptions = {instance={isVisible=function() return visible == true end}}
PZVRStereo = {
    setHandInteraction=function(...) handUse={...} end,
    setTurning=function(...) turning={...} end,
    setHotkeys=function(...) applied={...}; syncCount=(syncCount or 0)+1 end,
    blockHotkeys=function(value) blocked=value end,
    setArmReachPercent=function(value) armReach=value end,
    setAllowMotionMeleeWithGamepad=function(value) hybrid=value end,
    setControllerMode=function(value) controllerMode=value end,
    setMeleeMode=function(value) meleeMode=value end,
}
luautils = {split=function(str, separator)
    local result={}
    for value in string.gmatch(str, "[^" .. separator .. "]+") do table.insert(result,value) end
    return result
end}
saved=""
function getFileWriter(name, create, append)
    assert(name == "ModOptions.ini")
    saved=""
    return {write=function(self, value) saved=saved .. value end, close=function() end}
end
function getFileReader(name, create)
    assert(name == "ModOptions.ini")
    local lines={}
    for line in string.gmatch(saved, "[^\r\n]+") do table.insert(lines,line) end
    local index=0
    return {readLine=function() index=index+1; return lines[index] end, close=function() end}
end
''')
lua.execute(Path(sys.argv[1]).read_text(encoding="utf-8-sig"))
script = (root / "mod/42.20.4/media/lua/client/PZVROptions.lua").read_text()
lua.execute(script)
lua.execute(r'''
local options=PZAPI.ModOptions.Dict.PZ3DVRTest
assert(options.name == "PZ3D VR")
-- MainOptions passes slider names through getText; raw percent breaks missing-label formatting.
assert(not string.find(options:getOption("armReachPercent").name, "%", 1, true))
Events.OnMainMenuEnter.fire()
assert(applied[1]==70 and applied[2]==3 and applied[4]==7 and applied[6]==5 and applied[7]==68)
assert(armReach==150)
assert(handUse[1]==0 and handUse[2]==2)
options:getOption("handUseMode"):setValue(3)
options:getOption("handUseHand"):setValue(1)
options:apply()
assert(handUse[1]==2 and handUse[2]==0)
assert(turning[1]==0 and turning[2]==30 and turning[3]==90 and turning[4]==0 and turning[5]==0)
options:getOption("turnMode"):setValue(3)
options:getOption("turnAngle"):setValue(4)
options:getOption("turnSpeed"):setValue(120)
options:getOption("turnSource"):setValue(3)
options:getOption("turnAim"):setValue(4)
options:apply()
assert(turning[1]==2 and turning[2]==60 and turning[3]==120 and turning[4]==2 and turning[5]==3)
assert(meleeMode==0)
assert(controllerMode==0)
assert(hybrid==false)
options:getOption("meleeMode"):setValue(5); options:apply(); assert(meleeMode==4)
options:getOption("meleeMode"):setValue(1); options:apply()
local count=syncCount
-- Mimic vanilla's key-picker record, including the distinction between UI and saved key.
options:getOption("xr").element={keyCode=30}
options:getOption("xrModifiers"):setValue(1)
assert(syncCount==count and applied[1]==70)
options:apply()
assert(applied[1]==30 and applied[2]==0)
assert(options:getOption("xr"):getValue()==30)
options:getOption("capture").element={keyCode=0}
options:apply()
assert(applied[7]==0)
options:getOption("armReachPercent"):setValue(125)
options:getOption("meleeMode"):setValue(2)
options:getOption("controllerMode"):setValue(4)
options:getOption("allowMotionMeleeWithGamepad"):setValue(true)
options:apply(); assert(armReach==125)
assert(turning[1]==2 and turning[2]==60 and turning[3]==120 and turning[4]==2 and turning[5]==3)
assert(meleeMode==1)
assert(controllerMode==3)
assert(hybrid==true)
PZAPI.ModOptions:save()
options:getOption("handUseMode"):setValue(1)
options:getOption("handUseHand"):setValue(3)
assert(string.find(saved,"keybind|PZ3DVRTest|xr|30",1,true))
-- Simulate reloading persisted settings, rather than trusting in-memory values.
options:getOption("xr"):setValue(70)
options:getOption("xrModifiers"):setValue(4)
options:getOption("capture"):setValue(68)
options:getOption("armReachPercent"):setValue(150)
options:getOption("meleeMode"):setValue(1)
options:getOption("controllerMode"):setValue(1)
options:getOption("allowMotionMeleeWithGamepad"):setValue(false)
PZAPI.ModOptions:load()
assert(applied[1]==30 and applied[2]==0 and applied[7]==0)
assert(handUse[1]==2 and handUse[2]==0)
assert(armReach==125)
assert(turning[1]==2 and turning[2]==60 and turning[3]==120 and turning[4]==2 and turning[5]==3)
assert(meleeMode==1)
assert(controllerMode==3)
assert(hybrid==true)
visible=true; Events.OnTickEvenPaused.fire(); assert(blocked==true)
visible=false; typing=true; Events.OnTickEvenPaused.fire(); assert(blocked==true)
typing=false; Events.OnTickEvenPaused.fire(); assert(blocked==false)
-- Synthetic ownership confines disconnect cleanup; no physical controller cleanup.
local data={player=0,focus={onLoseJoypadFocus=function() lost=true end},disconnectedUI={removeFromUIManager=function() removed=true end}}
local controller={joypad=data}
JoypadState={controllers={[15]=controller},players={[1]=data},useKeyboardMouse=function() switched=true; data.player=nil end}
PZVRStereo.isBridgeController=function(id) return id==15 end
Events.OnGamepadDisconnect.fire(0); assert(not switched and not removed)
Events.OnGamepadDisconnect.fire(15); assert(switched and removed and lost)
assert(controller.joypad==nil and data.controller==nil and data.disconnectedUI==nil)
-- The options can still load when the Java bridge is unavailable/disabled.
PZVRStereo=nil; Events.OnGameStart.fire(); Events.OnTickEvenPaused.fire(); options:apply()
''')
print("Mods options checks passed: defaults, Apply, clear, native save/load, UI/text-entry guards, missing bridge")
lua.execute(r'''
Events.OnPostUIDraw={Add=function(fn) drawStatus=fn end}
UIFont={Large=1}
function getCore() return {getScreenWidth=function() return 800 end} end
function getTextManager() return {DrawStringCentre=function(self, font, x, y, text, r, g, b, a)
    assert(text==statusText); draws=(draws or 0)+1
end} end
PZVRStereo={tickXR=function() end,recenterStatus=function() return statusText end}
''')
lua.execute((root / "mod/42.20.4/media/lua/client/PZVRStereoCapture.lua").read_text())
lua.execute(r'''
statusText=""; drawStatus(); assert(draws==nil)
statusText="Recenter in 5"; drawStatus(); assert(draws==2)
statusText="Recenter complete"; drawStatus(); assert(draws==4)
PZVRStereo=nil; drawStatus(); assert(draws==4)
''')
print("Recenter UI checks passed: hidden while idle, countdown/confirmation drawing, missing bridge")
