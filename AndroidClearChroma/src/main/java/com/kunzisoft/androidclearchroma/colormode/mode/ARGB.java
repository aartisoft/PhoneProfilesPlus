package com.kunzisoft.androidclearchroma.colormode.mode;

import android.graphics.Color;

import com.kunzisoft.androidclearchroma.R;
import com.kunzisoft.androidclearchroma.colormode.Channel;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for managed Alpha-Red-Green-Blue color mode
 * @author Pavel Sikun
 */
public class ARGB implements AbstractColorMode {

    @Override
    public List<Channel> getChannels() {
        List<Channel> list = new ArrayList<>();

        list.add(new Channel(R.string.acch_channel_alpha, 0, 255, new Channel.ColorExtractor() {
            @Override
            public int extract(int color) {
                return Color.alpha(color);
            }
        }));

        list.add(new Channel(R.string.acch_channel_red, 0, 255, new Channel.ColorExtractor() {
            @Override
            public int extract(int color) {
                return Color.red(color);
            }
        }));

        list.add(new Channel(R.string.acch_channel_green, 0, 255, new Channel.ColorExtractor() {
            @Override
            public int extract(int color) {
                return Color.green(color);
            }
        }));

        list.add(new Channel(R.string.acch_channel_blue, 0, 255, new Channel.ColorExtractor() {
            @Override
            public int extract(int color) {
                return Color.blue(color);
            }
        }));

        return list;
    }

    @Override
    public int evaluateColor(List<Channel> channels) {
        return Color.argb(
                channels.get(0).getProgress(),
                channels.get(1).getProgress(),
                channels.get(2).getProgress(),
                channels.get(3).getProgress());
    }
}