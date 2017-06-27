package enviromine.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;
import org.lwjgl.opengl.GL11;
import enviromine.client.gui.hud.HUDRegistry;
import enviromine.client.gui.hud.HudItem;
import enviromine.client.gui.hud.items.Debug_Info;
import enviromine.client.gui.hud.items.GasMaskHud;
import enviromine.client.gui.menu.EM_Button;
import enviromine.client.gui.menu.EM_Gui_Menu;
import enviromine.core.EM_Settings;
import enviromine.core.EnviroMine;
import enviromine.handlers.EM_StatusManager;
import enviromine.trackers.EnviroDataTracker;
import enviromine.utils.RenderAssist;
import enviromine.world.ClientQuake;

@SideOnly(Side.CLIENT)
public class Gui_EventManager
{
	
	int width, height;
	
	//Render HUD
	//Render Player
	
	// Button Functions
	GuiButton enviromine;
	
	// Captures the initiation of vanilla menus to render new buttons
	@SuppressWarnings("unchecked")
	@SubscribeEvent
	public void renderevent(InitGuiEvent.Post event)
	{
		width = event.getGui(width);
		height = event.getGui(height);
		
		if(event.gui instanceof GuiIngameMenu && !EM_Settings.voxelMenuExists)
		{
			String newPost = UpdateNotification.isNewPost() ? " " + net.minecraft.util.text.translation.I18n.translateToLocal("news.enviromine.newpost") : "";

			try
			{
				byte b0 = -16;
				//enviromine = new GuiButton(1348, width / 2 - 100, height / 4 + 24 + b0, StatCollector.translateToLocal("options.enviromine.menu.title") + newPost);
				enviromine = new EM_Button(1348, width / 2 - 100, height / 4 + 24 + b0, StatCollector.translateToLocal("options.enviromine.menu.title") , newPost);
				event.buttonList.set(1, new GuiButton(4, width / 2 - 100, height / 4 + 0 + b0, I18n.format("menu.returnToGame", new Object[0])));
				event.buttonList.add(enviromine);
			} catch(Exception e)
			{
				enviromine = new GuiButton(1348, width - 175, height - 30, 160, 20, StatCollector.translateToLocal("options.enviromine.menu.title") + newPost);
				EnviroMine.logger.log(Level.ERROR, "Error shifting Minecrafts Menu to add in new button: " + e);
				event.buttonList.add(enviromine);
			}
		}
	}
	
	// Used to capture when an Enviromine button is hit in a vanilla menu
	@SubscribeEvent
	public void action(ActionPerformedEvent.Post event)
	{
		if(event.gui instanceof GuiIngameMenu)
		{
			if(event.button.id == enviromine.id)
			{
				Minecraft.getMinecraft().displayGuiScreen(new EM_Gui_Menu(event.gui));
			}
			
		}
	}
	
	public static int scaleTranslateX, scaleTranslateY;
	
	private Minecraft mc = Minecraft.getMinecraft();
	
	public static final ResourceLocation guiResource = new ResourceLocation("enviromine", "textures/gui/status_Gui.png");
	public static final ResourceLocation blurOverlayResource = new ResourceLocation("enviromine", "textures/misc/blur.png");
	
	public static EnviroDataTracker tracker = null;
	
	/**
	 * All Enviromine Gui and Hud Items will render here
	 * @param event
	 */
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onGuiRender(RenderGameOverlayEvent.Post event)
	{
		if(event.type != ElementType.HELMET || event.isCancelable())
		{
			return;
		}
		
		mc.player.yOffset = 1.62F;
		if(ClientQuake.GetQuakeShake(mc.world, mc.player) > 0)
		{
			if(mc.player == null || mc.player.isPlayerSleeping() || !mc.player.onGround || (mc.currentScreen != null && mc.currentScreen.doesGuiPauseGame()))
			{
			} else
			{
				float shakeMult = ClientQuake.GetQuakeShake(mc.world, mc.player);
				
				double shakeSpeed = 2D * shakeMult;
				float offsetY = 0.2F * shakeMult;
				
				double shake = (int)(mc.world.getTotalWorldTime() % 24000L) * shakeSpeed;
				
				mc.player.yOffset -= (Math.sin(shake) * (offsetY / 2F)) + (offsetY / 2F);
				mc.player.cameraPitch = (float)(Math.sin(shake) * offsetY / 4F);
				mc.player.cameraYaw = (float)(Math.sin(shake) * offsetY / 4F);
			}
		}
		
		HUDRegistry.checkForResize();
		
		if(tracker == null)
		{
			if(!(EM_Settings.enableAirQ == false && EM_Settings.enableBodyTemp == false && EM_Settings.enableHydrate == false && EM_Settings.enableSanity == false))
			{
				//Minecraft.getMinecraft().fontRenderer.drawStringWithShadow("NO ENVIRONMENT DATA", xPos, (height - yPos) - 8, 16777215);
				tracker = EM_StatusManager.lookupTrackerFromUsername(this.mc.player.getCommandSenderName());
			} 
		} else if(tracker.isDisabled || !EM_StatusManager.trackerList.containsValue(tracker))
		{
			tracker = null;
		} else
		{
			
			HudItem.blinkTick++;
			
			
			// Render GasMask Overlays
			if(UI_Settings.overlay)
			{
				GasMaskHud.renderGasMask(mc);
			}
			
			// Render Hud Items	
			GL11.glPushMatrix();
			GL11.glDisable(GL11.GL_LIGHTING);
	        GL11.glColor4f(1F, 1F, 1F, 1F);

			for(HudItem huditem : HUDRegistry.getActiveHudItemList())
			{
				
				if(mc.playerController.isInCreativeMode() && !huditem.isRenderedInCreative())
				{
					continue;
				}

				if(mc.player.ridingEntity instanceof EntityLivingBase)
				{
					if(huditem.shouldDrawOnMount())
					{
						if(UI_Settings.overlay)
						{
							RenderAssist.bindTexture(huditem.getResource("TintOverlay"));
							huditem.renderScreenOverlay(HUDRegistry.screenWidth, HUDRegistry.screenHeight);
						}
						
						RenderAssist.bindTexture(huditem.getResource(""));
						
						//float transx = (float)(huditem.posX - (huditem.posX * UI_Settings.guiScale));
						//float transy = (float)(huditem.posY - (huditem.posY * UI_Settings.guiScale));
						
						//GL11.glTranslated(transx, transy, 0);
						
						//GL11.glScalef((float)UI_Settings.guiScale, (float)UI_Settings.guiScale, (float)UI_Settings.guiScale);
						
						huditem.fixBounds();
						huditem.render();
						
						
					}
				} else
				{
					if(huditem.shouldDrawAsPlayer())
					{
						if(UI_Settings.overlay)
						{
							RenderAssist.bindTexture(huditem.getResource("TintOverlay"));
							huditem.renderScreenOverlay(HUDRegistry.screenWidth, HUDRegistry.screenHeight);
						}
						
						RenderAssist.bindTexture(huditem.getResource(""));
						
						//float transx = (float)(huditem.posX - (huditem.posX * UI_Settings.guiScale));
						//float transy = (float)(huditem.posY - (huditem.posY * UI_Settings.guiScale));
						
						//GL11.glTranslated(transx, transy, 0);
						
						//GL11.glScalef((float)UI_Settings.guiScale, (float)UI_Settings.guiScale, (float)UI_Settings.guiScale);
						
						huditem.fixBounds();
						huditem.render();
						
						//GL11.glTranslated(0, 0, 0);
						
					}
				}
				
			}
			Debug_Info.ShowDebugText(event, mc);
			GL11.glPopMatrix();
		}
		
	}
	
	//TODO Was used for Debugging Gui
	/*@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent event)
	{
		if(event == null) return;
		if(event.gui == null) return;
		System.out.println(event.gui.getClass().getSimpleName().toString());
		if(event.gui instanceof GuiConfig)
		{
			GuiConfig guiConfig = (GuiConfig) event.gui;
			
			System.out.println("configID:"+guiConfig.configID +" modID:"+ guiConfig.modID);
			
			Iterator<IConfigElement> elements = guiConfig.configElements.iterator();
			
			while(elements.hasNext())
			{
				IConfigElement element = elements.next();
				
				
				System.out.println("element name:"+ element.getName() +" Type:"+ element.getType() + " QNamed:"+element.getQualifiedName());
				
			}
			
		}
	}*/
	
}
