package mchorse.bbs_mod.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.data.Angle;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class Draw
{
    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d)
    {
        renderBox(stack, x, y, z, w, h, d, 1, 1, 1);
    }

    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        renderBox(stack, x, y, z, w, h, d, r, g, b, 1F);
    }

    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        stack.push();
        stack.translate(x, y, z);
        float fw = (float) w;
        float fh = (float) h;
        float fd = (float) d;
        float t = 1 / 96F + (float) (Math.sqrt(w * w + h + h + d + d) / 2000);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        /* Pillars: fillBox(builder, -t, -t, -t, t, t, t, r, g, b, a); */
        fillBox(builder, stack, -t, -t, -t, t, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);

        /* Top */
        fillBox(builder, stack, -t, -t + fh, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t + fh, -t, t + fw, t + fh, t + fd, r, g, b, a);

        /* Bottom */
        fillBox(builder, stack, -t, -t, -t, t + fw, t, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t + fw, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t, t, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t, t + fd, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        stack.pop();
    }

    /**
     * Fill a quad for {@link net.minecraft.client.render.VertexFormats#POSITION_TEXTURE_COLOR_NORMAL}. Points should
     * be supplied in this order:
     *
     *     3 -------> 4
     *     ^
     *     |
     *     |
     *     2 <------- 1
     *
     * I.e. bottom left, bottom right, top left, top right, where left is -X and right is +X,
     * in case of a quad on fixed on Z axis.
     */
    public static void fillTexturedNormalQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float r, float g, float b, float a, float nx, float ny, float nz)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BL, 2 - BR, 3 - TR, 4 - TL */
        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x1, y1, z1).texture(u2, v2).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).normal(nx, ny, nz).next();

        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).normal(nx, ny, nz).next();
        builder.vertex(matrix4f, x3, y3, z3).texture(u1, v1).color(r, g, b, a).normal(nx, ny, nz).next();
    }

    /**
     * Fill a quad for {@link net.minecraft.client.render.VertexFormats#POSITION_TEXTURE_COLOR}. Points should
     * be supplied in this order:
     *
     *     3 -------> 4
     *     ^
     *     |
     *     |
     *     2 <------- 1
     *
     * I.e. bottom left, bottom right, top left, top right, where left is -X and right is +X,
     * in case of a quad on fixed on Z axis.
     */
    public static void fillTexturedQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BL, 2 - BR, 3 - TR, 4 - TL */
        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).next();
        builder.vertex(matrix4f, x1, y1, z1).texture(u2, v2).color(r, g, b, a).next();
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).next();

        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a);
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).texture(u1, v1).color(r, g, b, a);
    }

    public static void fillQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).next();
        builder.vertex(matrix4f, x2, y2, z2).color(r, g, b, a).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).next();
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).next();
        builder.vertex(matrix4f, x4, y4, z4).color(r, g, b, a).next();
    }

    public static void fillBoxTo(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a)
    {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Angle angle = Angle.angle(dx, dy, dz);

        stack.push();

        stack.translate(x1, y1, z1);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle.yaw));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle.pitch));

        fillBox(builder, stack, -thickness / 2, -thickness / 2, 0, thickness / 2, thickness / 2, (float) distance, r, g, b, a);

        stack.pop();
    }

    public static void fillBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b)
    {
        fillBox(builder, stack, x1, y1, z1, x2, y2, z2, r, g, b, 1F);
    }

    public static void fillBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a)
    {
        /* X */
        fillQuad(builder, stack, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
        fillQuad(builder, stack, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);

        /* Y */
        fillQuad(builder, stack, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        fillQuad(builder, stack, x2, y2, z1, x1, y2, z1, x1, y2, z2, x2, y2, z2, r, g, b, a);

        /* Z */
        fillQuad(builder, stack, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, r, g, b, a);
        fillQuad(builder, stack, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
    }

    public static void axes(MatrixStack stack, float length, float thickness)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Draw.axes(builder, stack, length, thickness);

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static void axes(BufferBuilder builder, MatrixStack stack, float length, float thickness)
    {
        fillBox(builder, stack, thickness, -thickness, -thickness, length, thickness, thickness, 1, 0, 0, 1);
        fillBox(builder, stack, -thickness, -thickness, -thickness, thickness, length, thickness, 0, 1, 0, 1);
        fillBox(builder, stack, -thickness, -thickness, thickness, thickness, thickness, length, 0, 0, 1, 1);
    }

    public static void coolerAxes(MatrixStack stack, float axisSize, float axisOffset)
    {
        final float outlineSize = axisSize + 0.005F;
        final float outlineOffset = axisOffset + 0.005F;

        coolerAxes(stack, axisSize, axisOffset, outlineSize, outlineOffset);
    }

    public static void coolerAxes(MatrixStack stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;
        outlineSize *= scale;
        outlineOffset *= scale;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        fillBox(builder, stack, 0, -outlineOffset, -outlineOffset, outlineSize, outlineOffset, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, 0, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, 0, outlineOffset, outlineOffset, outlineSize, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0, 0, 0);

        fillBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize, axisOffset, axisOffset, 1, 0, 0);
        fillBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize, axisOffset, 0, 1, 0);
        fillBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize, 0, 0, 1);
        fillBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1, 1, 1);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    /* Gizmo rendering methods for 3D manipulation */
    
    public static void renderGizmoSphere(MatrixStack stack, float radius, float r, float g, float b, float a)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        
        // Create a more detailed sphere using multiple horizontal and vertical rings
        int horizontalSegments = 12;
        int verticalSegments = 8;
        float thickness = 0.008F;
        
        // Horizontal rings
        for (int i = 0; i < horizontalSegments; i++)
        {
            float angle1 = (float) (i * 2 * Math.PI / horizontalSegments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / horizontalSegments);
            
            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);
            
            // Multiple horizontal rings at different heights
            for (int h = 0; h < verticalSegments; h++)
            {
                float height = (float) ((h - verticalSegments / 2) * 2 * radius / verticalSegments);
                float ringRadius = (float) Math.sqrt(radius * radius - height * height);
                
                if (ringRadius > 0.01F)
                {
                    float x1_scaled = x1 * ringRadius / radius;
                    float z1_scaled = z1 * ringRadius / radius;
                    float x2_scaled = x2 * ringRadius / radius;
                    float z2_scaled = z2 * ringRadius / radius;
                    
                    fillBox(builder, stack, x1_scaled - thickness, height - thickness, z1_scaled - thickness, 
                           x1_scaled + thickness, height + thickness, z1_scaled + thickness, r, g, b, a);
                    fillBox(builder, stack, x2_scaled - thickness, height - thickness, z2_scaled - thickness, 
                           x2_scaled + thickness, height + thickness, z2_scaled + thickness, r, g, b, a);
                }
            }
        }
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
    
    public static void renderRotationRing(MatrixStack stack, float radius, float r, float g, float b, float a, int activeAxis)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        
        int segments = 64; // More segments for smoother rings
        float thickness = 0.015F;
        float innerRadius = radius - thickness;
        float outerRadius = radius + thickness;
        
        // Adjust colors for active state
        float finalR = r, finalG = g, finalB = b;
        if (activeAxis != -1)
        {
            finalR = Math.min(1.0F, r + 0.4F);
            finalG = Math.min(1.0F, g + 0.4F);
            finalB = Math.min(1.0F, b + 0.4F);
        }
        
        // Render ring as a series of connected quads
        for (int i = 0; i < segments; i++)
        {
            float angle1 = (float) (i * 2 * Math.PI / segments);
            float angle2 = (float) ((i + 1) * 2 * Math.PI / segments);
            
            float x1_inner = (float) (Math.cos(angle1) * innerRadius);
            float z1_inner = (float) (Math.sin(angle1) * innerRadius);
            float x1_outer = (float) (Math.cos(angle1) * outerRadius);
            float z1_outer = (float) (Math.sin(angle1) * outerRadius);
            
            float x2_inner = (float) (Math.cos(angle2) * innerRadius);
            float z2_inner = (float) (Math.sin(angle2) * innerRadius);
            float x2_outer = (float) (Math.cos(angle2) * outerRadius);
            float z2_outer = (float) (Math.sin(angle2) * outerRadius);
            
            // Render ring segment as a quad
            fillBox(builder, stack, x1_inner, -thickness, z1_inner, x1_outer, thickness, z1_outer, finalR, finalG, finalB, a);
            fillBox(builder, stack, x2_inner, -thickness, z2_inner, x2_outer, thickness, z2_outer, finalR, finalG, finalB, a);
            
            // Connect the segments
            fillBox(builder, stack, x1_outer, -thickness, z1_outer, x2_outer, thickness, z2_outer, finalR, finalG, finalB, a);
            fillBox(builder, stack, x1_inner, -thickness, z1_inner, x2_inner, thickness, z2_inner, finalR, finalG, finalB, a);
        }
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
    
    public static void renderRotationArrow(MatrixStack stack, float radius, float r, float g, float b, float a)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        
        // Render a small arrow on the ring to indicate rotation direction
        float arrowLength = 0.08F;
        float arrowWidth = 0.03F;
        
        // Arrow pointing in positive rotation direction
        fillBox(builder, stack, radius + arrowLength/2, -arrowWidth/2, -arrowWidth/2, 
               radius + arrowLength, arrowWidth/2, arrowWidth/2, r, g, b, a);
        
        // Arrow tip
        fillBox(builder, stack, radius + arrowLength, -arrowWidth, -arrowWidth, 
               radius + arrowLength + arrowWidth, arrowWidth, arrowWidth, r, g, b, a);
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }
    
    public static void renderBasicGizmo(MatrixStack stack, float scale, int activeAxis)
    {
        float gizmoScale = scale * BBSSettings.axesScale.get();
        
        // Render central origin sphere (smaller, more prominent)
        renderGizmoSphere(stack, 0.04F * gizmoScale, 0.9F, 0.9F, 0.9F, 0.9F);
        
        // Render outer boundary sphere (more subtle)
        renderGizmoSphere(stack, 0.22F * gizmoScale, 0.6F, 0.6F, 0.6F, 0.4F);
        
        // Render rotation rings for X, Y, Z axes with better colors
        stack.push();
        
        // X-axis ring (magenta, horizontal)
        stack.multiply(RotationAxis.POSITIVE_Z.rotation((float) Math.PI / 2));
        renderRotationRing(stack, 0.18F * gizmoScale, 1.0F, 0.0F, 1.0F, 0.9F, activeAxis == 0 ? 0 : -1);
        // Add directional arrow for X-axis
        renderRotationArrow(stack, 0.18F * gizmoScale, 1.0F, 0.0F, 1.0F, 0.9F);
        
        stack.pop();
        stack.push();
        
        // Y-axis ring (green, vertical)
        renderRotationRing(stack, 0.18F * gizmoScale, 0.0F, 1.0F, 0.0F, 0.9F, activeAxis == 1 ? 1 : -1);
        // Add directional arrow for Y-axis
        renderRotationArrow(stack, 0.18F * gizmoScale, 0.0F, 1.0F, 0.0F, 0.9F);
        
        stack.pop();
        stack.push();
        
        // Z-axis ring (blue, forward)
        stack.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.PI / 2));
        renderRotationRing(stack, 0.18F * gizmoScale, 0.0F, 0.0F, 1.0F, 0.9F, activeAxis == 2 ? 2 : -1);
        // Add directional arrow for Z-axis
        renderRotationArrow(stack, 0.18F * gizmoScale, 0.0F, 0.0F, 1.0F, 0.9F);
        
        stack.pop();
        
        // Render the translation axes (smaller and more subtle)
        coolerAxes(stack, 0.15F * gizmoScale, 0.008F * gizmoScale);
    }
}