-- Java polls the physical chord on the render thread, independent of Viewpoint's Lua input filtering.
local previous = nil
local function drawCaptureStatus()
    if not ProjectViewpointVR then return end
    local text = ProjectViewpointVR.status()
    if not text or text == "" then return end
    if text ~= previous then
        print("[Project Viewpoint VR] " .. text)
        previous = text
    end
    getTextManager():DrawString(UIFont.Small, 17, 89, text, 0, 0, 0, 1)
    getTextManager():DrawString(UIFont.Small, 16, 88, text, 1, 1, 1, 1)
end
Events.OnPostUIDraw.Add(drawCaptureStatus)
