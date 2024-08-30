package online.happlay.chat.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PageSize {
	SIZE15(15), SIZE20(20), SIZE30(30), SIZE40(40), SIZE50(50);
	int size;

}