package mchorse.bbs_mod.ui.forms.editors.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.function.Supplier;

public class UIPickableFormRenderer extends UIFormRenderer
{
    public UIFormEditor formEditor;

    private boolean update;

    private StencilFormFramebuffer stencil = new StencilFormFramebuffer();
    private StencilMap stencilMap = new StencilMap();

    private IEntity target;
    private Supplier<Boolean> renderForm;
    
    /* Gizmo state management */
    private boolean showGizmo = false;
    private int activeAxis = -1; // -1 = none, 0 = X, 1 = Y, 2 = Z
    private String selectedBone = "";
    
    /* Gizmo interaction state */
    private boolean isDraggingGizmo = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int lastMouseX = 0;
    private int lastMouseY = 0;

    public UIPickableFormRenderer(UIFormEditor formEditor)
    {
        this.formEditor = formEditor;
    }

    public void updatable()
    {
        this.update = true;
    }

    public void setRenderForm(Supplier<Boolean> renderForm)
    {
        this.renderForm = renderForm;
    }

    public IEntity getTargetEntity()
    {
        return this.target == null ? this.entity : this.target;
    }

    public void setTarget(IEntity target)
    {
        this.target = target;
    }

    private void ensureFramebuffer()
    {
        this.stencil.setup(Link.bbs("stencil_form"));
        this.stencil.resizeGUI(this.area.w, this.area.h);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.ensureFramebuffer();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        // Check for gizmo interaction first
        if (this.showGizmo && context.mouseButton == 0)
        {
            int clickedAxis = this.raycastGizmo(context.mouseX, context.mouseY);
            if (clickedAxis != -1)
            {
                this.startGizmoDrag(clickedAxis, context);
                return true;
            }
        }
        
        // Handle existing stencil picking
        if (this.stencil.hasPicked() && context.mouseButton == 0)
        {
            Pair<Form, String> pair = this.stencil.getPicked();

            if (pair != null)
            {
                System.out.println("[GIZMO] Bone picked: " + pair.b + " from form: " + pair.a.getIdOrName());
                
                this.formEditor.pickFormFromRenderer(pair);
                
                // Update gizmo to show the selected bone
                if (!pair.b.isEmpty())
                {
                    this.setSelectedBone(pair.b);
                    System.out.println("[GIZMO] Gizmo now showing for bone: " + pair.b);
                }

                return true;
            }
        }

        return super.subMouseClicked(context);
    }
    
    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.isDraggingGizmo)
        {
            this.endGizmoDrag();
            return true;
        }
        
        return super.subMouseReleased(context);
    }

    @Override
    protected void renderUserModel(UIContext context)
    {
        if (this.form == null)
        {
            return;
        }

        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.target == null ? this.entity : this.target, context.batcher.getContext().getMatrices(), LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
            .camera(this.camera)
            .modelRenderer();

        if (this.renderForm == null || this.renderForm.get())
        {
            FormUtilsClient.render(this.form, formContext);

            if (this.form.hitbox.get())
            {
                this.renderFormHitbox(context);
            }
        }

        this.renderAxes(context);

        if (this.area.isInside(context))
        {
            GlStateManager._disableScissorTest();

            this.stencilMap.setup();
            this.stencil.apply();
            FormUtilsClient.render(this.form, formContext.stencilMap(this.stencilMap));

            this.stencil.pickGUI(context, this.area);
            this.stencil.unbind(this.stencilMap);

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);

            GlStateManager._enableScissorTest();
        }
        else
        {
            this.stencil.clearPicking();
        }
    }

    private void renderAxes(UIContext context)
    {
        Matrix4f matrix = this.formEditor.editor.getOrigin(context.getTransition());
        MatrixStack stack = context.render.batcher.getContext().getMatrices();

        stack.push();

        if (matrix != null)
        {
            MatrixStackUtils.multiply(stack, matrix);
        }

        /* Draw axes */
        if (UIBaseMenu.renderAxes)
        {
            RenderSystem.disableDepthTest();
            
            // Check if we should show the enhanced gizmo
            if (this.showGizmo && !this.selectedBone.isEmpty())
            {
                // Render the new enhanced gizmo
                System.out.println("[GIZMO] Rendering enhanced gizmo for bone: " + this.selectedBone + ", active axis: " + this.activeAxis);
                Draw.renderBasicGizmo(stack, 1.0F, this.activeAxis);
            }
            else
            {
                // Render the original simple axes
                System.out.println("[GIZMO] Rendering simple axes (showGizmo: " + this.showGizmo + ", selectedBone: '" + this.selectedBone + "')");
                Draw.coolerAxes(stack, 0.25F, 0.01F, 0.26F, 0.02F);
            }
            
            RenderSystem.enableDepthTest();
        }

        stack.pop();
    }

    private void renderFormHitbox(UIContext context)
    {
        float hitboxW = this.form.hitboxWidth.get();
        float hitboxH = this.form.hitboxHeight.get();
        float eyeHeight = hitboxH * this.form.hitboxEyeHeight.get();

        /* Draw look vector */
        final float thickness = 0.01F;
        Draw.renderBox(context.batcher.getContext().getMatrices(), -thickness, -thickness + eyeHeight, -thickness, thickness, thickness, 2F, 1F, 0F, 0F);

        /* Draw hitbox */
        Draw.renderBox(context.batcher.getContext().getMatrices(), -hitboxW / 2, 0, -hitboxW / 2, hitboxW, hitboxH, hitboxW);
    }

    @Override
    protected void update()
    {
        super.update();

        if (this.update && this.target != null)
        {
            this.form.update(this.entity);
        }
    }

    @Override
    public void render(UIContext context)
    {
        // Handle gizmo dragging during render
        if (this.isDraggingGizmo)
        {
            this.updateGizmoDrag(context);
        }
        
        super.render(context);

        if (!this.stencil.hasPicked())
        {
            return;
        }

        int index = this.stencil.getIndex();
        Texture texture = this.stencil.getFramebuffer().getMainTexture();
        Pair<Form, String> pair = this.stencil.getPicked();
        int w = texture.width;
        int h = texture.height;

        ShaderProgram previewProgram = BBSShaders.getPickerPreviewProgram();
        GlUniform target = previewProgram.getUniform("Target");

        if (target != null)
        {
            target.set(index);
        }

        RenderSystem.enableBlend();
        context.batcher.texturedBox(BBSShaders::getPickerPreviewProgram, texture.id, Colors.WHITE, this.area.x, this.area.y, this.area.w, this.area.h, 0, h, w, 0, w, h);

        if (pair != null)
        {
            String label = pair.a.getIdOrName();

            if (!pair.b.isEmpty())
            {
                label += " - " + pair.b;
            }

            context.batcher.textCard(label, context.mouseX + 12, context.mouseY + 8);
        }
    }

    @Override
    protected void renderGrid(UIContext context)
    {
        if (this.renderForm == null || this.renderForm.get())
        {
            super.renderGrid(context);
        }
    }
    
    /* Gizmo interaction methods */
    
    private int raycastGizmo(int mouseX, int mouseY)
    {
        // For now, this is a simplified raycast that checks if the mouse is near the gizmo center
        // In a full implementation, this would do proper 3D raycasting against the gizmo components
        
        if (!this.area.isInside(mouseX, mouseY))
        {
            return -1;
        }
        
        // Simple distance-based detection for testing
        // This will be enhanced in later steps with proper 3D raycasting
        int centerX = this.area.mx();
        int centerY = this.area.my();
        
        float distance = (float) Math.sqrt((mouseX - centerX) * (mouseX - centerX) + (mouseY - centerY) * (mouseY - centerY));
        
        System.out.println("[GIZMO] Raycast check - mouse: (" + mouseX + ", " + mouseY + "), center: (" + centerX + ", " + centerY + "), distance: " + distance);
        
        // If mouse is within gizmo radius (approximate)
        if (distance < 100) // 100 pixels radius for testing
        {
            // Simple axis selection based on mouse position relative to center
            int dx = mouseX - centerX;
            int dy = mouseY - centerY;
            
            int axis = (Math.abs(dx) > Math.abs(dy)) ? (dx > 0 ? 0 : 0) : (dy < 0 ? 1 : 1);
            System.out.println("[GIZMO] Raycast hit! Axis: " + axis);
            return axis;
        }
        
        return -1;
    }
    
    private void startGizmoDrag(int axis, UIContext context)
    {
        System.out.println("[GIZMO] Starting gizmo drag on axis: " + axis);
        this.isDraggingGizmo = true;
        this.activeAxis = axis;
        this.dragStartX = context.mouseX;
        this.dragStartY = context.mouseY;
        this.lastMouseX = context.mouseX;
        this.lastMouseY = context.mouseY;
        
        // TODO: Store initial transform values for relative manipulation
    }
    
    private void updateGizmoDrag(UIContext context)
    {
        if (!this.isDraggingGizmo)
        {
            return;
        }
        
        // Calculate mouse delta
        int deltaX = context.mouseX - this.lastMouseX;
        int deltaY = context.mouseY - this.lastMouseY;
        
        // For now, just update the visual feedback
        // In later steps, this will actually modify the bone transform
        
        // Store current mouse position for next frame
        this.lastMouseX = context.mouseX;
        this.lastMouseY = context.mouseY;
        
        // TODO: Apply transform changes to the selected bone
    }
    
    private void endGizmoDrag()
    {
        this.isDraggingGizmo = false;
        this.activeAxis = -1;
        
        // TODO: Commit changes to keyframe system
    }
    
    /* Gizmo control methods */
    
    public void setGizmoVisible(boolean visible)
    {
        this.showGizmo = visible;
    }
    
    public void setSelectedBone(String bone)
    {
        System.out.println("[GIZMO] setSelectedBone called with: '" + bone + "'");
        this.selectedBone = bone;
        this.showGizmo = !bone.isEmpty();
        System.out.println("[GIZMO] showGizmo set to: " + this.showGizmo);
    }
    
    public void setActiveAxis(int axis)
    {
        this.activeAxis = axis;
    }
    
    public boolean isGizmoVisible()
    {
        return this.showGizmo;
    }
    
    public String getSelectedBone()
    {
        return this.selectedBone;
    }
    
    public int getActiveAxis()
    {
        return this.activeAxis;
    }
}