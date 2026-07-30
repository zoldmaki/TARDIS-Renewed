package dev.zoldmaki.renewedtardis.sonic;

/** Extension point for server-side sonic block behavior. */
public interface ISonicBlockAction {
    SonicActionResult execute(SonicBlockContext context);
}
