require "ISUI/ISPanel"
require "ISUI/ISButton"
require "ISUI/ISTextEntryBox"
require "ISUI/ISComboBox"

local panel = nil
local previous = nil
local function drawStatus()
    if not ProjectViewpointVR then return end
    local text = ProjectViewpointVR.status()
    if text and text ~= previous then
        print("[Project Viewpoint VR] " .. text)
        previous = text
    end
    if text then
        getTextManager():DrawString(UIFont.Small,17,89,text,0,0,0,1)
        getTextManager():DrawString(UIFont.Small,16,88,text,1,1,1,1)
    end
end
local function controls()
    local angles={15,30,45,60,90}
    local turnSelection={2,2,5}
    local savedScale=1
    if ProjectViewpointVR then
        turnSelection[1]=ProjectViewpointVR.savedTurnMode()+1
        local savedAngle=ProjectViewpointVR.savedSnapAngle()
        for index,angle in ipairs(angles) do if angle==savedAngle then turnSelection[2]=index end end
        turnSelection[3]=(ProjectViewpointVR.savedSmoothSpeed()-30)/15+1
        savedScale=ProjectViewpointVR.savedWorldScale()
    end
    if panel then panel:removeFromUIManager() end
    panel=ISPanel:new(16,112,410,166)
    panel:initialise()
    panel.moveWithMouse=true
    panel:addToUIManager()
    local modes={{"Off","OFF"},{"Desktop stereo","DESKTOP"},{"XR fixed","XR_FIXED"},{"XR tracked","XR_TRACKED"}}
    for index,mode in ipairs(modes) do
        local id=mode[2]
        local button=ISButton:new(6+(index-1)*100,6,96,24,mode[1],nil,function() if ProjectViewpointVR then ProjectViewpointVR.mode(id) end end)
        button:initialise();panel:addChild(button)
    end
    local recenter=ISButton:new(6,36,150,24,"Recenter in 5 seconds",nil,function() if ProjectViewpointVR then ProjectViewpointVR.recenter() end end)
    recenter:initialise();panel:addChild(recenter)
    local capture=ISButton:new(162,36,140,24,"Save PNG pair (Off)",nil,function() if ProjectViewpointVR then ProjectViewpointVR.requestCapture() end end)
    capture:initialise();panel:addChild(capture)
    local scale=ISTextEntryBox:new(tostring(savedScale),6,68,70,24)
    scale:initialise();panel:addChild(scale)
    local apply=ISButton:new(82,68,225,24,"Apply units/meter (while Off)",nil,function()
        local value=tonumber(scale:getText())
        if value and ProjectViewpointVR then print("[Project Viewpoint VR] "..ProjectViewpointVR.scale(value)) end
    end)
    apply:initialise();panel:addChild(apply)
    local inputModes={{"Controllers Off",0},{"UI pointer",1},{"Pointer + move/turn",2}}
    for index,mode in ipairs(inputModes) do
        local value=mode[2]
        local button=ISButton:new(6+(index-1)*133,102,129,24,mode[1],nil,function() if ProjectViewpointVR then ProjectViewpointVR.controllers(value) end end)
        button:initialise();panel:addChild(button)
    end
    local turnBoxes={}
    local function turningChanged()
        for index,box in ipairs(turnBoxes) do turnSelection[index]=box.selected end
        if ProjectViewpointVR then
            ProjectViewpointVR.turning(turnSelection[1]-1,angles[turnSelection[2]],30+(turnSelection[3]-1)*15)
        end
    end
    for index=1,3 do
        local box=ISComboBox:new(6+(index-1)*133,136,129,24,nil,turningChanged)
        box:initialise()
        if index==1 then
            for _,label in ipairs({"Turning: Off","Turning: Snap","Turning: Smooth"}) do box:addOption(label) end
        elseif index==2 then
            for _,angle in ipairs(angles) do box:addOption("Snap: "..angle.." deg") end
        else
            for speed=30,240,15 do box:addOption("Smooth: "..speed.." deg/s") end
        end
        box.selected=turnSelection[index]
        turnBoxes[index]=box
        panel:addChild(box)
    end

end
Events.OnGameStart.Add(controls)
Events.OnPreUIDraw.Add(function() if ProjectViewpointVR then ProjectViewpointVR.uiBegin() end end)
Events.OnPostUIDraw.Add(function()
    drawStatus()
    if ProjectViewpointVR then
        ProjectViewpointVR.uiPointer(getMouseX(),getMouseY(),getCore():getScreenWidth(),getCore():getScreenHeight())
        ProjectViewpointVR.uiEnd();ProjectViewpointVR.tick()
    end
end)
Events.OnMainMenuEnter.Add(function()
    if ProjectViewpointVR then ProjectViewpointVR.mode("OFF") end
    if panel then panel:removeFromUIManager();panel=nil end
end)
