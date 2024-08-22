/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * This file is part of XiaomiHelper project
 * Copyright (C) 2023 HowieHChen, howie.dev@outlook.com

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.lackluster.mihelper.hook.rules.shared

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.factory.field
import com.highcapable.yukihookapi.hook.factory.method
import dev.lackluster.mihelper.data.Pref
import dev.lackluster.mihelper.data.Scope
import dev.lackluster.mihelper.utils.Device
import dev.lackluster.mihelper.utils.factory.hasEnable

object UnlockTaplusForPad : YukiBaseHooker() {
    override fun onHook() {
        hasEnable(Pref.Key.Settings.UNLOCK_TAPLUS_FOR_PAD, extraCondition = { Device.isPad }) {
            when (packageName) {
                Scope.TAPLUS -> {
                    "com.miui.contentextension.setting.activity.MainSettingsActivity".toClass().method {
                        name = "getFragment"
                    }.hook {
                        "miui.os.Build".toClass().field {
                            name = "IS_TABLET"
                            modifiers {
                                isStatic
                            }
                        }.get().set(false)
                    }
                }
                Scope.SETTINGS -> {
                    "com.android.settings.utils.SettingsFeatures".toClass().method {
                        name = "isNeedRemoveContentExtension"
                    }.hook {
                        replaceToFalse()
                    }
                }
            }
        }
    }
}