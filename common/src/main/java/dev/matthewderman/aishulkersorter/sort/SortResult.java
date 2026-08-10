package dev.matthewderman.aishulkersorter.sort;

public record SortResult(boolean success, int boxesSorted, int itemsMoved, String errorMessage) {

    public static SortResult success(int boxesSorted, int itemsMoved) {
        return new SortResult(true, boxesSorted, itemsMoved, null);
    }

    public static SortResult error(String message) {
        return new SortResult(false, 0, 0, message);
    }
}
