package com.constructora_backend.enums;

/** Imagen del proyecto sobre la que se colocan los puntos. */
public enum EscenaPunto360 {

    /** Imagen 360° del entorno: los puntos se ubican por ángulos (yaw / pitch). */
    ENTORNO,
    /** Vista aérea en 360°: igual que el entorno, por ángulos. */
    AEREA,
    /** Plano de urbanismo (imagen plana): los puntos se ubican en porcentajes (posX / posY). */
    URBANISMO,
    /** Imagen de las zonas destacadas (imagen plana): un botón por zona común, en porcentajes (posX / posY). */
    ZONAS
}
