package sn.tools.demo.players.panel;

import sn.tools.natives.swing.api.movie.Movie;

public interface PlayerPanel {

    Movie getMovieComponent();

    public static String formatTime(long time) {
        long hours = time / 3600000;
        long minutes = (time % 3600000) / 60000;
        long seconds = (time % 60000) / 1000;
        return String.format("%2d:%02d:%02d", hours, minutes, seconds);
    }

}
