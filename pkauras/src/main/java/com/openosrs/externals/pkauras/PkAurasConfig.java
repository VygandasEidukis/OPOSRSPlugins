/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.openosrs.externals.pkauras;

import net.runelite.client.config.*;

@ConfigGroup("PkAurasConfig")

public interface PkAurasConfig extends Config
{
    @ConfigSection(
            position = 0,
            keyName = "botConfig",
            name = "Bot Config",
            description = ""
    )
    String mainConfig = "Bot Config";

    @ConfigItem(
            keyName = "toggle",
            name = "Toggle",
            description = "",
            position = 1,
            section = mainConfig
    )
    default Keybind toggle()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
            position = 2,
            keyName = "randLow",
            name = "Minimum Delay",
            description = "",
            section = mainConfig
    )
    default int randLow()
    {
        return 70;
    }

    @ConfigItem(
            position = 3,
            keyName = "randLower",
            name = "Maximum Delay",
            description = "",
            section = mainConfig
    )
    default int randHigh()
    {
        return 80;
    }

    String prayerSwitchDelayConfig = "Prayer swapping delay settings";
    @ConfigItem(
            position = 7,
            keyName = "randLowPraySwap",
            name = "Minimum Delay prayer swap",
            description = "",
            section = prayerSwitchDelayConfig
    )
    default int randLowSwapPrayer() { return 200; }

    @ConfigItem(
            position = 8,
            keyName = "randHighPraySwap",
            name = "Maximum Delay prayer swap",
            description = "",
            section = prayerSwitchDelayConfig
    )
    default int randHighSwapPrayer()
    {
        return 400;
    }
}