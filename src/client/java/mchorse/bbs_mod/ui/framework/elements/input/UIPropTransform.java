package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIPropTransform extends UITransform
{
    private static final double[] CURSOR_X = new double[1];
    private static final double[] CURSOR_Y = new double[1];

    private Transform transform;
    private Consumer<Transform> callback;

    private boolean editing;
    private int mode;
    private Axis axis = Axis.X;
    private int lastX;
    private Transform cache = new Transform();
    private Timer checker = new Timer(30);

    private boolean model;
    private boolean local;

    private UITransformHandler handler;

    public UIPropTransform()
    {
        this.handler = new UITransformHandler(this);

        this.context((menu) ->
        {
            menu.action(
                this.local ? Icons.FULLSCREEN : Icons.MINIMIZE,
                this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL,
                this::toggleLocal
            );

            menu.actions.add(0, menu.actions.remove(menu.actions.size() - 1));
        });

        this.iconT.callback = (b) -> this.toggleLocal();
        this.iconT.hoverColor = Colors.LIGHTEST_GRAY;
        this.iconT.setEnabled(true);
        this.iconT.tooltip(this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL);

        this.noCulling();
    }

    public UIPropTransform(Consumer<Transform> callback)
    {
        this();

        this.callback = callback;
    }

    public void setModel()
    {
        this.model = true;
    }

    public boolean isLocal()
    {
        return this.local;
    }

    private void toggleLocal()
    {
        this.local = !this.local;

        if (!this.local)
        {
            this.fillT(this.transform.translate.x, this.transform.translate.y, this.transform.translate.z);
        }

        this.tx.forcedLabel(this.local ? UIKeys.GENERAL_X : null);
        this.ty.forcedLabel(this.local ? UIKeys.GENERAL_Y : null);
        this.tz.forcedLabel(this.local ? UIKeys.GENERAL_Z : null);
        this.tx.relative(this.local);
        this.ty.relative(this.local);
        this.tz.relative(this.local);
        this.iconT.tooltip(this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL);
    }

    private Vector3f calculateLocalVector(double factor, Axis axis)
    {
        Vector3f vector3f = new Vector3f(
            (float) (axis == Axis.X ? factor : 0D),
            (float) (axis == Axis.Y ? factor : 0D),
            (float) (axis == Axis.Z ? factor : 0D)
        );
        /* I have no fucking idea why I have to rotate it 180 degrees by X axis... but it works! */
        Matrix3f matrix = new Matrix3f().rotateX(this.model ? MathUtils.PI : 0F).mul(this.transform.createRotationMatrix());

        matrix.transform(vector3f);

        return vector3f;
    }

    public UIPropTransform enableHotkeys()
    {
        IKey category = UIKeys.TRANSFORMS_KEYS_CATEGORY;
        Supplier<Boolean> active = () -> this.editing;

        this.keys().register(Keys.TRANSFORMATIONS_TRANSLATE, () -> this.enableMode(0)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_SCALE, () -> this.enableMode(1)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_ROTATE, () -> this.enableMode(2)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_X, () -> this.axis = Axis.X).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_Y, () -> this.axis = Axis.Y).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_Z, () -> this.axis = Axis.Z).active(active).category(category);

        return this;
    }

    public Transform getTransform()
    {
        return this.transform;
    }

    public void setTransform(Transform transform)
    {
        this.transform = transform;

        float minScale = Math.min(transform.scale.x, Math.min(transform.scale.y, transform.scale.z));
        float maxScale = Math.max(transform.scale.x, Math.max(transform.scale.y, transform.scale.z));

        if (BBSSettings.uniformScale.get())
        {
            if (
                (minScale == maxScale && !this.isUniformScale()) ||
                (minScale != maxScale && this.isUniformScale())
            ) {
                this.toggleUniformScale();
            }
        }

        this.fillT(transform.translate.x, transform.translate.y, transform.translate.z);
        this.fillS(transform.scale.x, transform.scale.y, transform.scale.z);
        this.fillR(MathUtils.toDeg(transform.rotate.x), MathUtils.toDeg(transform.rotate.y), MathUtils.toDeg(transform.rotate.z));
        this.fillR2(MathUtils.toDeg(transform.rotate2.x), MathUtils.toDeg(transform.rotate2.y), MathUtils.toDeg(transform.rotate2.z));
    }

    private void enableMode(int mode)
    {
        UIContext context = this.getContext();

        if (this.editing)
        {
            Axis[] values = Axis.values();

            this.axis = values[MathUtils.cycler(this.axis.ordinal() + 1, 0, values.length - 1)];

            this.restore(true);
            this.submit();
        }
        else
        {
            this.axis = Axis.X;
            this.lastX = context.mouseX;
        }

        this.editing = true;
        this.mode = mode;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }
    }

    /**
     * Begin translate gizmo editing from external UI (e.g., clicking axes in the preview).
     */
    public void beginTranslate()
    {
        this.enableMode(0);
    }

    /**
     * Begin scale gizmo editing from external UI.
     */
    public void beginScale()
    {
        this.enableMode(1);
    }

    /**
     * Begin rotate gizmo editing from external UI.
     */
    public void beginRotate()
    {
        this.enableMode(2);
    }

    /**
     * Force current axis for gizmo editing.
     */
    public void setAxis(Axis axis)
    {
        this.axis = axis;
    }

    private Vector3f getValue()
    {
        if (this.mode == 1)
        {
            return this.transform.scale;
        }
        else if (this.mode == 2)
        {
            return this.transform.rotate;
        }

        return this.transform.translate;
    }

    private void restore(boolean fully)
    {
        if (this.mode == 0 || fully) this.setT(null, this.cache.translate.x, this.cache.translate.y, this.cache.translate.z);
        if (this.mode == 1 || fully) this.setS(null, this.cache.scale.x, this.cache.scale.y, this.cache.scale.z);
        if (this.mode == 2 || fully) this.setR(null, MathUtils.toDeg(this.cache.rotate.x), MathUtils.toDeg(this.cache.rotate.y), MathUtils.toDeg(this.cache.rotate.z));
    }

    private void disable()
    {
        this.editing = false;

        if (this.handler.hasParent())
        {
            this.handler.removeFromParent();
        }
    }

    public void acceptChanges()
    {
        this.disable();
        this.submit();
        this.setTransform(this.transform);
    }

    public void rejectChanges()
    {
        this.disable();
        this.restore(true);
        this.submit();
        this.setTransform(this.transform);
    }

    @Override
    protected void internalSetT(double x, Axis axis)
    {
        if (this.local)
        {
            try
            {
                Vector3f vector3f = this.calculateLocalVector(x, axis);

                this.setT(null,
                    this.transform.translate.x + vector3f.x,
                    this.transform.translate.y + vector3f.y,
                    this.transform.translate.z + vector3f.z
                );
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            super.internalSetT(x, axis);
        }
    }

    @Override
    public void setT(Axis axis, double x, double y, double z)
    {
        this.transform.translate.set((float) x, (float) y, (float) z);
        this.submit();
    }

    @Override
    public void setS(Axis axis, double x, double y, double z)
    {
        this.transform.scale.set((float) x, (float) y, (float) z);
        this.submit();
    }

    @Override
    public void setR(Axis axis, double x, double y, double z)
    {
        this.transform.rotate.set(MathUtils.toRad((float) x), MathUtils.toRad((float) y), MathUtils.toRad((float) z));
        this.submit();
    }

    @Override
    public void setR2(Axis axis, double x, double y, double z)
    {
        this.transform.rotate2.set(MathUtils.toRad((float) x), MathUtils.toRad((float) y), MathUtils.toRad((float) z));
        this.submit();
    }

    protected void submit()
    {
        if (this.callback != null)
        {
            this.callback.accept(this.transform);
        }
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.editing)
        {
            if (context.isPressed(GLFW.GLFW_KEY_ENTER))
            {
                this.acceptChanges();

                return true;
            }
            else if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
            {
                this.rejectChanges();

                return true;
            }
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.editing && this.checker.isTime())
        {
            /* UIContext.mouseX can't be used because when cursor is outside of window
             * its position stops being updated. That's why it has to be queried manually
             * through GLFW...
             *
             * It gets updated outside the window only when one of mouse buttons is
             * being held! */
            GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);

            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getWidth();

            double rawX = CURSOR_X[0];
            double fx = Math.ceil(w / (double) context.menu.width);
            int border = 5;
            int borderPadding = border + 1;

            if (rawX <= border)
            {
                Window.moveCursor(w - borderPadding, (int) mc.mouse.getY());

                this.lastX = context.menu.width - (int) (borderPadding / fx);
                this.checker.mark();
            }
            else if (rawX >= w - border)
            {
                Window.moveCursor(borderPadding, (int) mc.mouse.getY());

                this.lastX = (int) (borderPadding / fx);
                this.checker.mark();
            }
            else
            {
                int dx = context.mouseX - this.lastX;
                Vector3f vector = this.getValue();
                boolean all = Window.isAltPressed();

                float factor = this.mode == 0 ? 0.05F : (this.mode == 1 ? 0.01F : 0.5F);

                if (this.local && this.mode == 0)
                {
                    Vector3f vector3f = this.calculateLocalVector(factor * dx, this.axis);

                    this.setT(null, vector.x + vector3f.x, vector.y + vector3f.y, vector.z + vector3f.z);
                }
                else
                {
                    Vector3f vector3f = new Vector3f(vector);

                    if (this.mode == 2)
                    {
                        vector3f.mul(180F / MathUtils.PI);
                    }

                    if (this.axis == Axis.X || all) vector3f.x += factor * dx;
                    if (this.axis == Axis.Y || all) vector3f.y += factor * dx;
                    if (this.axis == Axis.Z || all) vector3f.z += factor * dx;

                    if (this.mode == 0) { this.setT(null, vector3f.x, vector3f.y, vector3f.z); }
                    if (this.mode == 1) { this.setS(null, vector3f.x, vector3f.y, vector3f.z); }
                    if (this.mode == 2) { this.setR(null, vector3f.x, vector3f.y, vector3f.z); }
                }

                this.setTransform(this.transform);

                this.lastX = context.mouseX;
            }
        }

        super.render(context);

        if (this.editing)
        {
            String label = UIKeys.TRANSFORMS_EDITING.get();
            FontRenderer font = context.batcher.getFont();
            int x = this.area.mx(font.getWidth(label));
            int y = this.area.my(font.getHeight());

            context.batcher.textCard(label, x, y, Colors.WHITE, BBSSettings.primaryColor(Colors.A50));
        }
    }

    public static class UITransformHandler extends UIElement
    {
        private UIPropTransform transform;

        public UITransformHandler(UIPropTransform transform)
        {
            this.transform = transform;
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            // Right-click cancels during drag
            if (this.transform.editing && context.mouseButton == 1)
            {
                this.transform.rejectChanges();
                return true;
            }
            
            return super.subMouseClicked(context);
        }

        @Override
        protected boolean subMouseReleased(UIContext context)
        {
            // Left mouse release commits changes
            if (this.transform.editing && context.mouseButton == 0)
            {
                this.transform.acceptChanges();
                return true;
            }

            return super.subMouseReleased(context);
        }
    }
}