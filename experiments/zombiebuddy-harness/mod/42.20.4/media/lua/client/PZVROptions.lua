require "PZAPI/ModOptions"

local options = PZAPI.ModOptions:create("PZ3DVRTest", "PZ3D VR")
options:addDescription("Choose a keyboard key and modifiers for each shortcut. Press the key alone in the picker, then select modifiers below. Use Clear in the picker to disable a shortcut. Changes take effect with Apply. Identical shortcuts are disabled until changed.")
local rows = {
    {"xr", "Toggle OpenXR", Keyboard.KEY_SCROLL, 3},
    {"recenter", "Recenter headset", Keyboard.KEY_SCROLL, 7},
    {"preview", "Toggle synthetic arms (XR) / desktop stereo (XR off)", Keyboard.KEY_SCROLL, 5},
    {"capture", "Save stereo PNG pair (XR off)", Keyboard.KEY_F10, 3},
}
local modifiers = {"None", "Ctrl", "Shift", "Ctrl + Shift", "Alt", "Ctrl + Alt", "Shift + Alt", "Ctrl + Shift + Alt"}
for _, row in ipairs(rows) do
    options:addKeyBind(row[1], row[2], row[3])
    local combo = options:addComboBox(row[1] .. "Modifiers", row[2] .. " - modifiers")
    for index, label in ipairs(modifiers) do combo:addItem(label, index == row[4] + 1) end
end
options:addDescription("Hold the selected modifiers before pressing the key. Extra modifiers do not match. Choose keys that do not conflict with your game or other mods; these shortcuts do not consume native controls.")
options:addSeparator()
options:addSlider("armReachPercent", "Maximum arm reach (percent)", 100, 175, 5, 150)
options:addDescription("Arms extend only when needed to reach the tracked controllers, up to this percentage of the character's normal arm length. Hands and items keep their size. 100 restores the original reach limit. Higher limits can visibly stretch sleeves and elbows.")
options:addSeparator()
local melee = options:addComboBox("meleeMode", "Motion melee prototype")
melee:addItem("Off", true)
melee:addItem("Diagnostics only (no attacks)", false)
melee:addItem("Live armed melee (animation-timed)", false)
melee:addItem("Bat contact diagnostics (no attacks)", false)
melee:addItem("Bat contact-timed attacks", false)
options:addDescription("Contact modes: plain Base.BaseballBat against standing zombies only. Hold the right trigger and swing the rendered bat into a target; release between attacks. Contact resolves through native combat as soon as its attack state is ready, within 150 ms. Approximate bat/body capsules; no headshot, floor, scenery, or multiplayer contact attacks. Enable Allow motion melee with gamepad input for hybrid movement.")
options:addDescription("Hold the right trigger and swing the primary weapon hand. Release the trigger between swings; keep holding until the native attack finishes. Uses character facing and animation-timed hits. Swing weapons only; no firearms, shoves, stomps, knives, spears, or chainsaws in this prototype. Begin with Diagnostics only and inspect [PZ3D VR Melee] in console.txt.")

options:addSeparator()
local controller = options:addComboBox("controllerMode", "VR controllers as gamepad")
controller:addItem("Off", true)
controller:addItem("Input diagnostics only", false)
controller:addItem("Gamepad", false)
controller:addItem("Gamepad with diagnostics", false)
options:addDescription("Quest Touch: enable OpenXR, return controls to neutral, then enable/assign PZ VR Gamepad in vanilla controller settings. Menu tap = Start; hold Menu + left stick = D-pad; Menu + X = Back. The runtime may reserve Menu. Off neutralizes an already registered gamepad until restart. Diagnostics appear in console.txt.")

options:addTickBox("allowMotionMeleeWithGamepad", "Allow motion melee with gamepad input", false)
options:addDescription("When checked, gamepad movement and buttons remain available alongside the selected Motion melee mode. While motion melee is set to Diagnostics or Live, the right trigger is reserved for it and native gamepad RT stays released, including in menus and for firearms. Set Motion melee to Off to restore RT. Return controls to neutral after changing this setting. Unchecked preserves gamepad-only behavior.")

local turn = options:addComboBox("turnMode", "Stick turning")
for _, label in ipairs({"Off", "Snap", "Smooth"}) do turn:addItem(label, label == "Off") end
local angle = options:addComboBox("turnAngle", "Snap angle")
for _, value in ipairs({15, 30, 45, 60, 90}) do angle:addItem(tostring(value) .. " degrees", value == 30) end
options:addSlider("turnSpeed", "Smooth turn speed (degrees/second)", 30, 240, 15, 90)
local source = options:addComboBox("turnSource", "Turning input")
for _, label in ipairs({"Automatic", "VR controllers", "Assigned gamepad"}) do source:addItem(label, label == "Automatic") end
local aim = options:addComboBox("turnAim", "Hold ready/aim")
for _, label in ipairs({"Left trigger", "Left bumper / left grip", "Right bumper / right grip", "Right stick click"}) do aim:addItem(label, label == "Left trigger") end
options:addDescription("While XR gameplay is active, reserve the right stick for horizontal turning and the selected binding for ready/aim. Automatic prefers the player's assigned physical gamepad, otherwise VR controllers. Snap once per deflection; center the stick to rearm. Menus retain native controls. Turning Off restores native aiming. Turning temporarily disarms motion melee; release the attack trigger after the turn to rearm. Return controls to neutral after settings changes.")

options:addSeparator()
local handUse = options:addComboBox("handUseMode", "Hand proximity interaction")
for _, label in ipairs({"Off", "Diagnostics only", "Live"}) do handUse:addItem(label, label == "Off") end
local useHand = options:addComboBox("handUseHand", "Interaction grip")
for _, label in ipairs({"Either hand", "Left hand", "Right hand"}) do useHand:addItem(label, label == "Right hand") end
options:addDescription("Reach within 25 cm of a reachable world object and squeeze the selected grip once to use it. Release between presses. Uses PZ3D's normal short-press action, including native door/window handling. No hold-to-climb action. Diagnostics log the target without using it. Requires tracked controllers; synthetic arms cannot interact.")
options:addDescription("While enabled, the selected grip is reserved for interaction, including in menus: its virtual gamepad bumper and VR ready/aim binding are disabled. Choose a different ready/aim binding (for example Left trigger), or set interaction Off to restore the grip. Physical gamepads are unchanged. Single player prototype; logs use [PZ3D VR Use].")

local function sync()
    if not PZVRStereo or not PZVRStereo.setHotkeys then return end
    if PZVRStereo.setHandInteraction then PZVRStereo.setHandInteraction(options:getOption("handUseMode"):getValue() - 1, options:getOption("handUseHand"):getValue() - 1) end
    if PZVRStereo.setTurning then
        local angles = {15, 30, 45, 60, 90}
        PZVRStereo.setTurning(options:getOption("turnMode"):getValue() - 1, angles[options:getOption("turnAngle"):getValue()], options:getOption("turnSpeed"):getValue(), options:getOption("turnSource"):getValue() - 1, options:getOption("turnAim"):getValue() - 1)
    end
    local values = {}
    for _, row in ipairs(rows) do
        table.insert(values, options:getOption(row[1]):getValue())
        table.insert(values, options:getOption(row[1] .. "Modifiers"):getValue() - 1)
    end
    PZVRStereo.setHotkeys(unpack(values))
    if PZVRStereo.setArmReachPercent then
        PZVRStereo.setArmReachPercent(math.floor(options:getOption("armReachPercent"):getValue() + 0.5))
    end
    if PZVRStereo.setAllowMotionMeleeWithGamepad then PZVRStereo.setAllowMotionMeleeWithGamepad(options:getOption("allowMotionMeleeWithGamepad"):getValue()) end
    if PZVRStereo.setControllerMode then PZVRStereo.setControllerMode(options:getOption("controllerMode"):getValue() - 1) end
    if PZVRStereo.setMeleeMode then PZVRStereo.setMeleeMode(options:getOption("meleeMode"):getValue() - 1) end
end
function options:apply()
    -- Vanilla's mod key picker edits its UI record separately from the saved option.
    for _, row in ipairs(rows) do
        local option = self:getOption(row[1])
        if option.element then option:setValue(option.element.keyCode) end
    end
    sync()
end
local nativeLoad = PZAPI.ModOptions.load
function PZAPI.ModOptions:load(...)
    nativeLoad(self, ...)
    sync()
end
Events.OnMainMenuEnter.Add(sync)
Events.OnGameStart.Add(sync)

local function guardSettings()
    if not PZVRStereo or not PZVRStereo.blockHotkeys then return end
    local visible = MainOptions and MainOptions.instance and MainOptions.instance:isVisible()
    PZVRStereo.blockHotkeys(visible == true or getCore():isDoingTextEntry())
end
Events.OnTickEvenPaused.Add(guardSettings)
Events.OnMainMenuEnter.Add(guardSettings)

-- Only invoked when a physical pad claims the bridge slot. Native disconnect runs first.
Events.OnGamepadDisconnect.Add(function(id)
    if not PZVRStereo or not PZVRStereo.isBridgeController or not PZVRStereo.isBridgeController(id) then return end
    if not JoypadState then return end
    local controller = JoypadState.controllers[id]
    if controller and controller.joypad then
        local data = controller.joypad
        if data.disconnectedUI then
            data.disconnectedUI:removeFromUIManager()
            data.disconnectedUI = nil
        end
        if data.player == 0 then
            if ISJoypadDisconnectedUI and ISJoypadDisconnectedUI.setKeyboardMouseActivated then
                ISJoypadDisconnectedUI.setKeyboardMouseActivated()
            else
                JoypadState.useKeyboardMouse()
            end
        end
        if data.focus then data.focus:onLoseJoypadFocus(data) end
        data.focus = nil
        if data.player then JoypadState.players[data.player + 1] = nil end
        data.player = nil
        data.controller = nil
        controller.joypad = nil
    end
end)
