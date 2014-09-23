package nlp;

import java.util.ArrayList;
import java.util.List;

public class ReVerbPattern {
	
	public List<String> token_words = new ArrayList<String>();
	public List<String> token_universal_pos_tags = new ArrayList<String>();
	public List<String> token_ptb_pos_tags = new ArrayList<String>();
	
	public ReVerbPattern(List<String> token_words, List<String> token_universal_pos_tags, List<String> token_ptb_pos_tags) {
		super();
		this.token_words = token_words;
		this.token_universal_pos_tags = token_universal_pos_tags;
		this.token_ptb_pos_tags = token_ptb_pos_tags;
	}
}
