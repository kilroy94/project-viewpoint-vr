require "ISUI/ISPanel"
require "ISUI/ISButton"
require "ISUI/ISTextEntryBox"
require "ISUI/ISComboBox"
require "ISUI/ISScrollingListBox"

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
        local px,py=ProjectViewpointVR.diagnosticPointerX(),ProjectViewpointVR.diagnosticPointerY()
        if px>=0 and py>=0 then getTextManager():DrawString(UIFont.Medium,px*getCore():getScreenWidth()-5,py*getCore():getScreenHeight()-10,"+",0.1,0.9,1,1) end
        local diagnostic=ProjectViewpointVR.diagnosticStatus()
        if diagnostic then getTextManager():DrawString(UIFont.Small,16,68,diagnostic,0.1,0.9,1,1) end
        getTextManager():DrawString(UIFont.Small,17,89,text,0,0,0,1)
        getTextManager():DrawString(UIFont.Small,16,88,text,1,1,1,1)
    end
end
local diagnosticPanel=nil
local function openDiagnostic()
    if diagnosticPanel then diagnosticPanel:removeFromUIManager() end
    local width,height=getCore():getScreenWidth(),getCore():getScreenHeight()
    local test=ISPanel:new(math.max(16,math.min(440,width-430)),math.max(16,math.min(112,height-410)),410,390)
    diagnosticPanel=test;test:initialise();test:addToUIManager();test:setAlwaysOnTop(true)
    test.backgroundColor={r=0.08,g=0.08,b=0.1,a=1}
    local count=0
    local click=ISButton:new(20,55,260,30,"Test click: 0",nil,function()
        count=count+1
        test.click:setTitle("Test click: "..count)
        ProjectViewpointVR.diagnosticEvent("vanilla","click",count)
    end)
    click:initialise();test:addChild(click);test.click=click
    local drag=ISPanel:new(20,105,280,45);drag:initialise();test:addChild(drag);drag.value=0.25;drag.held=false
    function drag:onMouseDown(x,y) self.held=true;self:setCapture(true);return true end
    function drag:onMouseMove(dx,dy)
        if self.held then
            self.value=math.max(0,math.min(1,self:getMouseX()/self.width))
            ProjectViewpointVR.diagnosticEvent("vanilla","drag",self.value)
        end
        return true
    end
    drag.onMouseMoveOutside=drag.onMouseMove
    function drag:onMouseUp(x,y)
        if self.held then self.held=false;self:setCapture(false);ProjectViewpointVR.diagnosticEvent("vanilla","release",self.value) end
        return true
    end
    drag.onMouseUpOutside=drag.onMouseUp
    function drag:prerender()
        ISPanel.prerender(self)
        self:drawRect(self.value*(self.width-12),2,12,self.height-4,1,0.1,0.9,1)
        self:drawText("Test drag",10,12,1,1,1,1,UIFont.Small)
    end
    local scroll=ISScrollingListBox:new(20,175,300,100);scroll:initialise();test:addChild(scroll);scroll.lastOffset=0
    for row=1,35 do scroll:addItem("Diagnostic scroll row "..row,row) end
    function scroll:prerender()
        ISScrollingListBox.prerender(self)
        local offset=self:getYScroll()
        if offset~=self.lastOffset then self.lastOffset=offset;ProjectViewpointVR.diagnosticEvent("vanilla","scroll",offset) end
    end
    local start=ISButton:new(20,305,175,25,"Start scripted UI test",nil,function()
        scroll:setYScroll(0);scroll.smoothScrollTargetY=nil;scroll.smoothScrollY=nil;drag.value=.25
        print("[Project Viewpoint VR] "..ProjectViewpointVR.diagnosticStart("vanilla"))
    end)
    start:initialise();test:addChild(start)
    local stop=ISButton:new(205,305,175,25,"Stop / Close test",nil,function()
        ProjectViewpointVR.diagnosticStop();drag:setCapture(false);test:removeFromUIManager();diagnosticPanel=nil
    end)
    stop:initialise();test:addChild(stop)
    function test:prerender()
        ISPanel.prerender(self)
        self:drawText("Controller diagnostic - Zomboid UI",15,10,1,1,1,1,UIFont.Medium)
        self:drawText("XR active, Controllers Off, cursor released.",15,32,1,1,1,1,UIFont.Small)
        self:drawText("Release mouse; keep this panel still for ~25 seconds.",15,344,1,1,1,1,UIFont.Small)
        self:drawText("Pause/Break or Stop cancels. Report saved as .txt.",15,365,1,1,1,1,UIFont.Small)
        local x,y=self:getAbsoluteX(),self:getAbsoluteY()
        ProjectViewpointVR.diagnosticTargets("vanilla",x+150,y+70,x+70,y+127,x+220,y+127,x+120,y+220,getCore():getScreenWidth(),getCore():getScreenHeight())
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
    panel=ISPanel:new(16,112,410,198)
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

    local testButton=ISButton:new(6,168,260,24,"Open controller diagnostic",nil,function() if ProjectViewpointVR then openDiagnostic() end end)
    testButton:initialise();panel:addChild(testButton)
    local stopTest=ISButton:new(272,168,132,24,"Stop diagnostic",nil,function() if ProjectViewpointVR then ProjectViewpointVR.diagnosticStop() end end)
    stopTest:initialise();panel:addChild(stopTest)

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
    if diagnosticPanel then diagnosticPanel:removeFromUIManager();diagnosticPanel=nil end
    if panel then panel:removeFromUIManager();panel=nil end
end)
