package org.vectomatic.svg.chess;

import com.alonsoruibal.chess.Board;
import com.alonsoruibal.chess.search.SearchObserver;

public interface SuperSearchObserver extends SearchObserver {
	Board getBoard();
	void restart();
	void setFEN(String string);
	void xBoardUndo();
	void xBoardRemove();
	void setClientOpponentTime(int value);
	void setClientPlayerTime(int value);
	void enableTime(int minutes);
}
