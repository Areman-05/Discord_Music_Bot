/*
 * Copyright 2018 SoundBot Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.soundbot.gui;

import com.soundbot.Bot;
import javax.swing.JFrame;

/**
 *
 * @author SoundBot Contributors
 */
public class GUI extends JFrame
{
    private final Bot bot;
    
    public GUI(Bot bot)
    {
        this.bot = bot;
    }
    
    public void init()
    {
        // GUI initialization can be added here if needed
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("SoundBot");
        setVisible(false);
    }
    
    public void dispose()
    {
        super.dispose();
    }
}

