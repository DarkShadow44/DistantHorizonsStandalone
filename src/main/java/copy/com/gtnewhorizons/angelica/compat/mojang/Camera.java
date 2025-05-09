package copy.com.gtnewhorizons.angelica.compat.mojang;

import com.gtnewhorizon.gtnhlib.blockpos.BlockPos;
import com.seibel.distanthorizons.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class Camera {
    final Vector3d pos = new Vector3d();
    final BlockPos blockPos = new BlockPos();
    float pitch;
    float yaw;
    EntityLivingBase entity;
    boolean thirdPerson;
    final float partialTicks;

    public Vector3d getPos()
    {
        return pos;
    }

    public Camera(EntityLivingBase entity, float partialTicks) {
        this.partialTicks = partialTicks;
        final Vector4f offset = new Vector4f(); // third person offset
        final Matrix4f inverseModelView = RenderHelper.getModelViewMatrixMC().invert();
        inverseModelView.transform(offset);

        final double camX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks + offset.x;
        final double camY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks + offset.y;
        final double camZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks + offset.z;
        this.entity = entity;

        pos.set(camX, camY, camZ);
        blockPos.set(MathHelper.floor_double(camX), MathHelper.floor_double(camY), MathHelper.floor_double(camZ));
        pitch = entity.cameraPitch;
        yaw = entity.rotationYaw;
        thirdPerson = Minecraft.getMinecraft().gameSettings.thirdPersonView == 1;

    }

}
