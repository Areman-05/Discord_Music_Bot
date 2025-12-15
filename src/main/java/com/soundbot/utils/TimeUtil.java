/*
 * Copyright 2020 SoundBot Contributors
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
package com.soundbot.utils;

public class TimeUtil
{

    public static String formatTime(long duration)
    {
        if(duration == Long.MAX_VALUE)
            return "LIVE";
        long seconds = Math.round(duration/1000.0);
        long hours = seconds/(60*60);
        seconds %= 60*60;
        long minutes = seconds/60;
        seconds %= 60;
        return (hours>0 ? hours+":" : "") + (minutes<10 ? "0"+minutes : minutes) + ":" + (seconds<10 ? "0"+seconds : seconds);
    }

    /**
     * Parses a seek time string into milliseconds and determines if it's relative.
     * Supports "colon time" (HH:MM:SS) or "unit time" (1h20m)
     * @param args time string
     * @return SeekTime object, or null if the string could not be parsed
     */
    public static SeekTime parseTime(String args)
    {
        if (args == null || args.length() == 0) return null;
        String timestamp = args;
        boolean relative = false; // seek forward or backward
        boolean isSeekingBackwards = false;
        char first = timestamp.charAt(0);
        if (first == '+' || first == '-')
        {
            relative = true;
            isSeekingBackwards = first == '-';
            timestamp = timestamp.substring(1);
            if(timestamp.isEmpty()) return null;
        }

        long milliseconds = parseColonTime(timestamp);
        if(milliseconds == -1) milliseconds = parseUnitTime(timestamp);
        if(milliseconds == -1) return null;

        milliseconds *= isSeekingBackwards ? -1 : 1;

        return new SeekTime(milliseconds, relative);
    }

    /**
     * @param timestamp timestamp formatted as: [+ | -] &lt;HH:MM:SS | MM:SS | SS&gt;
     * @return Time in milliseconds
     */
    public static long parseColonTime(String timestamp)
    {
        if(timestamp == null || timestamp.isEmpty())
            return -1;
        String[] timestampSplitArray = timestamp.split(":+");
        if(timestampSplitArray.length > 3 || timestampSplitArray.length == 0)
            return -1;
        double[] timeUnitArray = new double[3]; // hours, minutes, seconds
        for(int index = 0; index < timestampSplitArray.length; index++)
        {
            String unit = timestampSplitArray[index];
            if(unit == null || unit.isEmpty() || unit.startsWith("+") || unit.startsWith("-")) 
                return -1;
            unit = unit.replace(",", ".");
            try
            {
                timeUnitArray[index + 3 - timestampSplitArray.length] = Double.parseDouble(unit);
            }
            catch (NumberFormatException e)
            {
                return -1;
            }
        }
        return Math.round(timeUnitArray[0] * 3600000 + timeUnitArray[1] * 60000 + timeUnitArray[2] * 1000);
    }

    /**
     *
     * @param timestr time string formatted as a unit time, e.g. 20m10, 1d5h20m14s or 1h and 20m
     * @return Time in milliseconds
     */
    public static long parseUnitTime(String timestr)
    {
        if(timestr == null || timestr.isEmpty())
            return -1;
        timestr = timestr.replaceAll("(?i)(\\s|,|and)","")
                .replaceAll("(?is)(-?\\d+|[a-z]+)", "$1 ")
                .trim();
        if(timestr.isEmpty())
            return -1;
        String[] vals = timestr.split("\\s+");
        if(vals.length == 0)
            return -1;
        int time = 0;
        try
        {
            for(int j=0; j<vals.length; j+=2)
            {
                if(j >= vals.length || vals[j] == null || vals[j].isEmpty())
                    return -1;
                int num = Integer.parseInt(vals[j]);

                if(vals.length > j+1 && vals[j+1] != null)
                {
                    String unit = vals[j+1].toLowerCase();
                    if(unit.startsWith("m"))
                        num*=60;
                    else if(unit.startsWith("h"))
                        num*=60*60;
                    else if(unit.startsWith("d"))
                        num*=60*60*24;
                }

                time+=num*1000;
            }
        }
        catch(Exception ex)
        {
            return -1;
        }
        return time;
    }

    public static class SeekTime
    {
        public final long milliseconds;
        public final boolean relative;

        private SeekTime(long milliseconds, boolean relative)
        {
            this.milliseconds = milliseconds;
            this.relative = relative;
        }
    }
}

