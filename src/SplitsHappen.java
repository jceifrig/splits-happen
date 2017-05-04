import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;


/**
 * SplitsHappen: a bowling score calculator, part of the EITC programming quiz.
 * 
 * Usage: java SplitsHappen [-v]
 * Reads score as a single line from standard input.  Outputs calculated score
 * on standard output.
 * If the '-v' option is passed, SplitsHappen outputs the tokenized input broken
 * into bowling frames, and the score for each frame as well.
 * 
 * Bugs and known issues: Java's BufferedReader.readLine and Eclipse's input
 * redirection facility don't cooperate: if you're running this from within
 * Eclipse and redirecting stdin from a file, the file needs to end in a newline
 * character or the program will hang in readScoreLine();
 * 
 * Principles of operation: The app reads the entire input and then processes
 * it in phases:
 *   1: Tokenize the score line into a series of frames, each consisting of a
 *      single character (an 'X', indicating a strike) or two characters
 *      (indicating either a split or a spare).
 *   2: For each frame, calculate the score for the frame.  In the cases of
 *      strikes and spares, this involves looking ahead one or two frames to
 *      determine the values of the bonus.
 *   3: Add the scores for each frame into a total score for the game.
 *   
 *   As an example, this program takes the sample input of 'X7/9-X-88/-6XXX81'
 *   and processes it as:
 *   Frame #:   Frame:  Score:
 *      1:        X       10 + 7 + 3   = 20
 *      2:        7/      10 + 3       = 19
 *      3:        9-      9 + 0        =  9
 *      4:        X       10 + 0 + 8   = 18
 *      5:        -8      0 + 8        =  8
 *      6:        8/      10 + 0       = 10
 *      7:        -6      0 + 6        =  6
 *      8:        X       10 + 10 + 10 = 30
 *      9:        X       10 + 10 + 8  = 28
 *     10:        X       10 + 8 + 1   = 19
 *    Bonus:      81      --
 *  TOTAL:                              167
 *   
 * This approach is verbose, but it has a couple of advantages:
 *   1: It makes it possible to simply output the score for each frame, the
 *      traditional way bowling scores are recorded.
 *   2: It makes validating the input much easier (although not implemented
 *      here.  For example, a score line like '7X....' is invalid; this is
 *      easy to detect by breaking the input into frames as a first step, but
 *      detecting this in a state machine approach is much more cumbersome. 
 * 
 * As this is a quick-and-dirty implementation for a programming quiz, numerous
 * shortcuts have been taken: the result is basically FORTRAN-translated-to-Java:
 *   - All methods are static.
 *   - Frames are encoded as Strings, rather than having their own DTO
 *     representation.
 *   
 * 
 * @author jonathaneifrig
 * Date: 04-MAY-2017
 */
public class SplitsHappen {
	private static boolean verbose = false;

	public static void main(String[] args) throws Exception {
		parseArgs(args);
		String scoreLine = readScoreLine();
		List<String> frames = tokenizeScoreLine(scoreLine);
		int score = scoreFrames(frames);
		System.out.format("%d\n", score);
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
	 * Read the input as a single line from stdin.  As noted above in the usage
	 * notes, there's a bug in either BufferedReader.readLine() or in Eclipse's
	 * input redirection facility that will cause this to hang unless the
	 * input file ends in a newline.
	 * @return
	 * @throws IOException
	 */
	private static String readScoreLine() throws IOException {
		Reader reader = new InputStreamReader(System.in);
		BufferedReader bufferedReader = new BufferedReader(reader);
		// @TODO: Figure out why this hangs in Eclipse with redirected stdin:
		String res = bufferedReader.readLine();
		return res;
	}
	
	
	/**
	 * Tokenize a score line into a series of frames.  Each frame is encoded as
	 * a string of 1 or 2 characters, each representing a ball of the frame.
	 * @param scoreLine
	 * @return
	 */
	private static List<String> tokenizeScoreLine(String scoreLine) {
		List<String> res = new ArrayList<String>(13);
		if (scoreLine == null) return res;
		for (int i=0; i<scoreLine.length();) {
			char c = scoreLine.charAt(i);
			String frame;
			switch (c) {
			case 'X':
				frame = scoreLine.substring(i, i+1);
				res.add(frame);
				i += 1;
				break;
			default:
				// String.substring has this irritating inability to specify an
				// ending length beyond the end of the source string without
				// throwing an error, which is why this silly Integer.min()
				// conditioning of the scanner index is necessary:
				frame = scoreLine.substring(i, Integer.min(i+2, scoreLine.length()));
				res.add(frame);
				i += 2;
				break;
			}
		}
		if (verbose) {
			System.out.format("Frames:");
			for (String f : res) {
				System.out.format(" %s", f);
			}
			System.out.format("\n");
		}
		return res;
	}
	
	/**
	 * Calculate an overall score for a series of frames.  There are three
	 * types of frames:
	 *  - Strikes (encoded as "X").
	 *  - Spares (encoded as ".\").
	 *  - Splits (encoded as "<a><b>").
	 * @param frames
	 * @return
	 */
	private static int scoreFrames(List<String> frames) {
		int res = 0;
		int frameScore;
		for (int i=0; i<10; i++) {
			String frame = frames.get(i);
			if ("X".equals(frame)) {
				frameScore = scoreStrike(i, frames);
			} else if (frame.length() == 2) {
				char c = frame.charAt(1);
				if ('/' == c) {
					frameScore = scoreSpare(i, frames);
				} else {
					frameScore = scoreSplit(i, frames);
				}
			} else {
				throw new IllegalArgumentException("Illegal frame: '"+frame+"'");
			}
			if (verbose) {
				System.out.format("  %2d: %2s: %2d\n", i, frame, frameScore);
			}
			res += frameScore;
		}
		return res;
	}
	
	/**
	 * Calculate the score for a strike. Each strike is worth 10 pts, plus a
	 * bonus equal to the next two balls.
	 * @param frameNumber
	 * @param frames
	 * @return
	 */
	private static int scoreStrike(int frameNumber, List<String> frames) {
		int firstBonus = getFirstBonusBall(frameNumber+1, frames);
		int secondBonus = getSecondBonusBall(frameNumber+1, frames);
		int res = 10 + firstBonus + secondBonus;
		return res;
	}
	
	/**
	 * Calculate the score for a spare. Each spare is worth 10 pts, plus a
	 * bonus equal to the next ball.
	 * @param frameNumber
	 * @param frames
	 * @return
	 */
	private static int scoreSpare(int frameNumber, List<String> frames) {
		int bonus = getFirstBonusBall(frameNumber +1, frames);
		int res = 10 + bonus;
		return res;
	}
	
	/**
	 * Score a split. Each split is worth the sum of the two ball scores
	 * within it.
	 * @param frameNumber
	 * @param frames
	 * @return
	 */
	private static int scoreSplit(int frameNumber, List<String> frames) {
		String frame = frames.get(frameNumber);
		char ballOne = frame.charAt(0);
		char ballTwo = frame.charAt(1);
		int res = scoreBall(ballOne) + scoreBall(ballTwo);
		return res;
	}
	
	/**
	 * Get the value of the first bonus ball starting in the indicated frame;
	 * this is needed for scoring spares and strikes. The first bonus ball is
	 * easy: it's directly encoded in the first character of the frame.
	 * 
	 * @param frameNumber
	 * @param frames
	 * @return
	 */
	private static int getFirstBonusBall(int frameNumber, List<String> frames) {
		String bonusFrame = frames.get(frameNumber);
		char c = bonusFrame.charAt(0);
		int res = scoreBall(c);
		return res;
	}
	
	/**
	 * Get the value of the second bonus ball starting in the indicated frame;
	 * this is necessary for scoring strikes.  The second bonus ball is a bunch
	 * of special cases:
	 *  - If the starting frame is a strike, the bonus ball is the first ball of
	 *    the following frame.
	 *  - If the starting frame is a spare, the bonus ball isn't directly
	 *    recorded in the input; we have to calculate it as 10 minus the first
	 *    ball of the frame.
	 *  - If the starting frame is a split, the bonus ball is just the second
	 *    ball of the frame.
	 * @param frameNumber
	 * @param frames
	 * @return
	 */
	private static int getSecondBonusBall(int frameNumber, List<String> frames) {
		String firstBonusFrame = frames.get(frameNumber);
		if ("X".equals(firstBonusFrame)) {
			String secondBonusFrame = frames.get(frameNumber+1);
			char c = secondBonusFrame.charAt(0);
			int res = scoreBall(c);
			return res;
		} else {
			char secondBall = firstBonusFrame.charAt(1);
			if (secondBall == '/') {
					char previousBall = firstBonusFrame.charAt(0);
					int res = 10 - scoreBall(previousBall);
					return res;
			}
			else {
				int res = scoreBall(secondBall);
				return res;
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
