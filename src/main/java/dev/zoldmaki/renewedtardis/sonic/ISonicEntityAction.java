package dev.zoldmaki.renewedtardis.sonic;

/** Extension point for server-side sonic entity behavior. */
public interface ISonicEntityAction {
    SonicActionResult execute(SonicEntityContext context);
}
