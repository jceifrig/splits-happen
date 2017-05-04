import java.io.InputStreamReader;
import java.io.Reader;

/**
 * SplitsHappenFSA: a bowling score calculator, part of the EITC programming quiz.
 * 
 * Usage: java SplitsHappenFSA [-v]
 * Reads score as a single line from standard input.  Outputs calculated score
 * on standard output.
 * If the '-v' option is passed, SplitsHappenFSA outputs the per-ball score and
 * the running total, along with the bowling frame count.
 * 
 * Principles of operation: SplitsHappenFSA operates on the input as a stream
 * via a finite-state machine. Each character of input represents a throw of the
 * player's ball.  The FSA calculates a running total for the player as it
 * processes each character of input. As it's processing the input, the machine
 * maintains the following state variables:
 * 
 *  - 'lastBall': The number of pins knocked down on the last ball (initially
 *    zero). This is needed in order to process spares ('/' characters).
 *  - 'currentBonus': This is the multiplier to be applied to the current ball
 *    score due to previous strikes and spares.
 *  - 'nextBonus': This is the multiplier to be applied to the next ball score
 *    due to previous strikes.
 *  - 'inSplit': Is the current ball the second ball of a split or spare? This
 *    is necessary in order to maintain the frame count as process the input.
 *  - 'frameCount': The number of frames processed.  After 10 frames, the FSA
 *    needs to switch to bonus scoring: spares and strikes no longer provide a
 *    bonus for later balls.
 *  - inBaseGame: Is the FSA in base-game mode or final-bonus scoring mode?
 *    While logically a boolean state variable, it is stored as an integer (1 ==
 *    in base game, 0 == in in final-bonus mode) in order to simplify the
 *    expression to calculate the score of a ball.
 * 
 *   As an example, this program takes the sample input of 'X7/9-X-88/-6XXX81'
 *   and processes it as:
 *   Input #:  Ball:  Frame:  Ball Score:  Running Score:  
 *     1:        X      1      10 * 1 = 10     10
 *     2:        7      2       7 * 2 = 14     24
 *     3:        /      2       3 * 2 =  6     30
 *     4:        9      3       9 * 2 = 18     48
 *     5:        -      3       0 * 1 =  0     48
 *     6:        X      4      10 * 1 = 10     58
 *     7:        -      5       0 * 2 =  0     58
 *     8:        8      5       8 * 2 = 16     74
 *     9:        8      6       8 * 1 =  8     82
 *    10:        /      6       2 * 1 =  2     84
 *    11:        -      7       0 * 2 =  0     84
 *    12:        6      7       6 * 1 =  6     90
 *    13:        X      8      10 * 1 = 10    100
 *    14:        X      9      10 * 2 = 20    120
 *    15:        X     10      10 * 3 = 30    150
 *    16:        8    Bonus     8 * 2 = 16    166
 *    17:        1    Bonus     1 * 1 =  1    167
 *   TOTAL:                                   167
 *   
 *   This approach is alien to how human bowlers score games, but it has a
 *   couple of advantages:
 *    - It's much more terse than the more traditional approach of breaking
 *      the games into frames and then scoring each frame individually.
 *    - Processing input as a stream/FSA means that there's no need to read
 *      the entire input and hold it in memory while processing.  This is
 *      obviously of no concern when processing a 10 frame bowling game, but
 *      the principle of stream processing would make it possible to operate
 *      on arbitrarily long input streams.
 *   
 * @author jonathaneifrig
 * Date: 04-MAY-2017
 */
public class SplitsHappenFSA {

	private static boolean verbose = false;
	
	public static void main(String[] args) throws Exception {
		parseArgs(args);
		
		Reader inputReader = new InputStreamReader(System.in);
		
		int lastBall = 0;
		int currentBonus = 0;
		int nextBonus = 0;
		int currentScore = 0;
		int frameCount = 0;
		boolean inSplit = false;
		int inBaseGame = 1;
		int i=0;
		
		int baseScore;
		int ballScore;
		while (inBaseGame > 0 || currentBonus > 0 || nextBonus > 0) {
			int charAsInt = inputReader.read();
			if (charAsInt == -1) {
				break;
			}
			char c = (char)charAsInt;
			switch (c) {
			case 'X':
				baseScore = scoreBall(c);
				ballScore = (inBaseGame + currentBonus) * baseScore;
				currentScore += ballScore;
				if (verbose) {
					System.out.format("%2d: %2d: %c: %2d (%3d)\n", i, frameCount, c, ballScore, currentScore);
				}
				inSplit = false;
				frameCount++;
				lastBall = baseScore;
				currentBonus = nextBonus + inBaseGame;
				nextBonus = inBaseGame;
				break;
			case '/':
				baseScore = 10 - lastBall;
				ballScore = (inBaseGame + currentBonus) * baseScore;
				currentScore += ballScore;
				if (verbose) {
					System.out.format("%2d: %2d: %c: %2d (%3d)\n", i, frameCount, c, ballScore, currentScore);
				}
				inSplit = false;
				frameCount++;
				lastBall = baseScore;
				currentBonus = nextBonus + inBaseGame;
				nextBonus = 0;
				break;
			default:
				baseScore = scoreBall(c);
				ballScore = (inBaseGame + currentBonus) * baseScore;
				currentScore += ballScore;
				if (verbose) {
					System.out.format("%2d: %2d: %c: %2d (%3d)\n", i, frameCount, c, ballScore, currentScore);
				}
				if (inSplit) {
					frameCount++;
					inSplit = false;
				} else {
					inSplit = true;
				}
				lastBall = baseScore;
				currentBonus = nextBonus;
				nextBonus = 0;
			}
			i++;
			if (frameCount>= 10) {
				inBaseGame = 0;
			}
		}
		System.out.format("%d\n", currentScore);
	}

	/**
	 * Parse the command-line arguments.  Currently, this just recognizes the
	 * "-v" argument for enabling verbose output.
	 * @param args
	 */
	private static void parseArgs(String[] args) {
		if (args == null) return;
		for (int i=0; i<args.length; ) {
			String arg = args[i];
			switch (arg) {
			case "-v":
				verbose=true;
				i++;
				break;
			default:
				throw new IllegalArgumentException("Unrecognized argument: '"+arg+"'");
			}
		}
	}
	
	/**
	 * Translate the character representation of a ball in the input to a score
	 * (equal to a number of pins).  This is normally a digit, except that
	 * misses are recorded as a '-' (rather than a '0'), and strikes are
	 * recorded as an 'X'.  While they appear in the input, '/' characters don't
	 * indicate a ball score and hence throw an IllegalArgumentException.
	 * @param c
	 * @return
	 */
	private static int scoreBall(char c) {
		switch (c) {
		case '-': return 0;
		case '1': return 1;
		case '2': return 2;
		case '3': return 3;
		case '4': return 4;
		case '5': return 5;
		case '6': return 6;
		case '7': return 7;
		case '8': return 8;
		case '9': return 9;
		case 'X': return 10;
		}
		throw new IllegalArgumentException("Invalid ball character: '"+c+"'");
	}
}
