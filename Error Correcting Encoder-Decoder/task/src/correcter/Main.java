package correcter;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Write a mode: ");
            String mode = scanner.nextLine();
            ProgramMode programMode = ProgramMode.getMode(mode);

            testStageFive(programMode);
        } catch(Exception e) {
            System.out.println("error:"+e.getMessage());
        }
    }

    public static void testStageTwo(ProgramMode programMode) throws IOException {
        ErrorCorrectionTemplate template = new ErrorCorrectionTemplate(
                DataManipulationScheme.SYMBOL, programMode, 3, 3);
        template.perform();
    }

    public static void testStageThree() throws IOException {
        ErrorCorrectionTemplate template = new ErrorCorrectionTemplate(
                DataManipulationScheme.BIT, ProgramMode.SEND_NO_ENCODE, -1, 8);
        template.perform();
    }

    public static void testStageFour() throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Write a mode: ");
        String mode = scanner.nextLine();
        ProgramMode programMode = ProgramMode.getMode(mode);
        ErrorCorrectionTemplate template = new ErrorCorrectionTemplate(
                DataManipulationScheme.BIT, programMode, 2, 8);
        template.perform();
    }

    public static void testStageFive(ProgramMode programMode) throws IOException {
        ErrorCorrectionTemplate template = new ErrorCorrectionTemplate(
                DataManipulationScheme.HAMMING, programMode, 1, 8);
        template.perform();
    }
}

class ErrorCorrectionTemplate {
    protected Operation operationMode;
    protected ProgramMode programMode;
    protected DataManipulationScheme scheme;

    protected int numberOfTimesToRepeat;
    protected int numberOfCharsForOneError;

    protected char[] input;
    protected char[] output;

    protected FileInputOutputUtil fileUtil;
    protected AbstractEncoderDecoderFactory factory;

    public ErrorCorrectionTemplate(DataManipulationScheme scheme, ProgramMode programMode,
                                   int numberOfTimesToRepeat, int numberOfCharsForOneError) {
        setScheme(scheme);
        setProgramMode(programMode);
        setNumberOfTimesToRepeat(numberOfTimesToRepeat);
        setNumberOfCharsForOneError(numberOfCharsForOneError);
        fileUtil = new FileInputOutputUtil(programMode);
    }

    public void setScheme(DataManipulationScheme scheme) {
        this.scheme = scheme;
        switch(scheme) {
            case SYMBOL:
                factory = new SymbolEncoderDecoderFactory();
                break;
            case BIT:
                factory = new BitEncoderDecoderFactory();
                break;
            case HAMMING:
                factory = new HammingEncoderDecoderFactory();
                break;
        }
    }

    public void setOperation(Operation operationMode) {
        this.operationMode = operationMode;
    }

    public void setProgramMode(ProgramMode programMode) {
        this.programMode = programMode;
        switch(programMode) {
            case ENCODE:
                setOperation(Operation.ENC);
                break;
            case SEND_NO_ENCODE:
            case SEND:
                setOperation(Operation.NONE);
                break;
            case DECODE:
                setOperation(Operation.DEC);
                break;
        }
    }

    public void setNumberOfTimesToRepeat(int numberOfTimesToRepeat) {
        this.numberOfTimesToRepeat = numberOfTimesToRepeat;
    }

    public void setNumberOfCharsForOneError(int numberOfCharsForOneError) {
        this.numberOfCharsForOneError = numberOfCharsForOneError;
    }

    public void setInput(char[] input) {
        this.input = input;
        output = Arrays.copyOf(input, input.length);
    }

    void encodeOrDecode() {

        if(programMode == ProgramMode.ENCODE || programMode == ProgramMode.DECODE) {
            EncoderDecoder encoderOrDecoder = factory.createEncoderDecoder(operationMode, numberOfTimesToRepeat);
            encoderOrDecoder.setInput(output);
            encoderOrDecoder.perform();
            output = encoderOrDecoder.getOutput();
        }
    }
    void addOrRemoveErrors() {
        if(programMode != ProgramMode.ENCODE) {
            ErrorSimulator simulator = factory.createErrorSimulator(numberOfCharsForOneError, numberOfTimesToRepeat);
            simulator.setInput(output);
            if(programMode == ProgramMode.SEND || programMode == ProgramMode.SEND_NO_ENCODE)
                simulator.addErrors();
            else
                simulator.removeErrors();
            output = simulator.getOutput();
        }

    }

    public void perform() throws IOException {
        readInput();
        if(scheme != DataManipulationScheme.SYMBOL)
            output = Converter.preprocessInput(output);

        if(programMode != ProgramMode.DECODE) {
            encodeOrDecode();
            addOrRemoveErrors();
        } else {
            addOrRemoveErrors();
            encodeOrDecode();
        }
        if(scheme != DataManipulationScheme.SYMBOL)
            output = Converter.postprocessOutput(output);
        writeOutput();
    }

    public void readInput() throws IOException {
        setInput(fileUtil.readAll());
    }

    public void writeOutput() throws IOException {
        fileUtil.writeAll(output);
    }
}

class FileInputOutputUtil {

    private static final String ORIGINAL_FILE = "send.txt";
    private static final String ENCODED_FILE = "encoded.txt";
    private static final String RECEIVED_FILE = "received.txt";
    private static final String DECODED_FILE = "decoded.txt";

    private static final int READ_MODE = 0;
    private static final int WRITE_MODE = 1;


    private ProgramMode programMode;

    public FileInputOutputUtil(ProgramMode programMode) {
        this.programMode = programMode;
    }

    private String getFileName(int mode) {
        String fileName = "";
        switch(programMode) {
            case ENCODE:
                fileName = mode == READ_MODE ? ORIGINAL_FILE : ENCODED_FILE;
                break;
            case SEND:
                fileName = mode == READ_MODE ? ENCODED_FILE : RECEIVED_FILE;
                break;
            case SEND_NO_ENCODE:
                fileName = mode == READ_MODE ? ORIGINAL_FILE : RECEIVED_FILE;
                break;
            case DECODE:
                fileName = mode == READ_MODE ? RECEIVED_FILE : DECODED_FILE;
                break;
        }
        return fileName;
    }

    private char[] getCharactersFromList(List<Character> characterList) {
        char[] characters = new char[characterList.size()];
        for(int i = 0; i < characters.length; i++) {
            characters[i] = characterList.get(i);
        }
        return characters;
    }



    public char[] readAll() throws IOException {
        String fileName = getFileName(READ_MODE);
        List<Character> characterList = new ArrayList<>();
        try( FileInputStream in = new FileInputStream(fileName) ) {
            int ch = in.read();
            while (ch != -1) {
                characterList.add((char)ch);
                ch = in.read();
            }
        }
        return getCharactersFromList(characterList);
    }

    public void writeAll(char[] inputBytes) throws IOException {
        String fileName = getFileName(WRITE_MODE);

        try(FileOutputStream out = new FileOutputStream(fileName, false)) {
            for(char b : inputBytes) {
                out.write(b);
            }
        }
    }
}



interface AbstractEncoderDecoderFactory {
    EncoderDecoder createEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat);
    ErrorSimulator createErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat);
}

class SymbolEncoderDecoderFactory implements AbstractEncoderDecoderFactory {

    @Override
    public EncoderDecoder createEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat) {
        return new SymbolLevelEncoderDecoder(operationMode, numberOfTimesToRepeat);
    }

    @Override
    public ErrorSimulator createErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat) {
        return new SymbolLevelErrorSimulator(numberOfCharsForOneError, numberOfTimesToRepeat);
    }
}

class BitEncoderDecoderFactory implements AbstractEncoderDecoderFactory {

    @Override
    public EncoderDecoder createEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat) {
        return new BitLevelEncoderDecoder(operationMode, numberOfTimesToRepeat);
    }

    @Override
    public ErrorSimulator createErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat) {
        return new BitLevelErrorSimulator(numberOfCharsForOneError, numberOfTimesToRepeat);
    }
}

class HammingEncoderDecoderFactory implements AbstractEncoderDecoderFactory {

    @Override
    public EncoderDecoder createEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat) {
        return new HammingLevelEncoderDecoder(operationMode, numberOfTimesToRepeat);
    }

    @Override
    public ErrorSimulator createErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat) {
        return new HammingLevelErrorSimulator(numberOfCharsForOneError, numberOfTimesToRepeat);
    }
}

interface Container {
    void setInput(char[] input);
    void setNumberOfTimesToRepeat(int numberOfTimesToRepeat);
    char[] getOutput();
}

abstract class ConcreteContainer implements Container {
    protected char[] input;
    protected char[] output;
    protected int numberOfTimesToRepeat;


    public void setInput(char[] input) {
        this.input = input;
    }

    public char[] getOutput() {
        return output;
    }

    public void setNumberOfTimesToRepeat(int numberOfTimesToRepeat) {
        this.numberOfTimesToRepeat = numberOfTimesToRepeat;
    }
}

interface EncoderDecoder extends Container {
    void setOperationMode(Operation operationMode);
    void encode();
    void decode();
    void perform();
}

abstract class ConcreteEncoderDecoder extends ConcreteContainer implements EncoderDecoder  {
    protected Operation operationMode;

    public void setOperationMode(Operation operationMode) {
        this.operationMode = operationMode;
    }

    public void perform() {
        if(operationMode == Operation.ENC)
            encode();
        else if(operationMode == Operation.DEC)
            decode();
    }
}

interface SymbolLevelConfiguration {
    static final String ALPHABETS = "abcdefghijklmnopqrstuvwxyz";
    static final char[] DIGITS = "0123456789".toCharArray();
    static final char[] LOWER_CASE = ALPHABETS.toCharArray();
    static final char[] UPPER_CASE = ALPHABETS.toUpperCase().toCharArray();
    public static final char[] SYMBOLS = new char[DIGITS.length+LOWER_CASE.length+UPPER_CASE.length+1];


}

interface BitLevelConfiguration {
    public static final char[] SYMBOLS = {'0', '1'};
    public static final int GROUP_BITS_ENCODE = 3;
}

interface HammingLevelConfiguration {
    public static final char[] SYMBOLS = {'0', '1'};
    public static final int GROUP_BITS_ENCODE = 4;
}

class SymbolLevelEncoderDecoder extends ConcreteEncoderDecoder implements SymbolLevelConfiguration{

    public SymbolLevelEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat) {
        setOperationMode(operationMode);
        setNumberOfTimesToRepeat(numberOfTimesToRepeat);
    }

    public SymbolLevelEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat, char[] input) {
        this(operationMode, numberOfTimesToRepeat);
        setInput(input);
    }

    private void buildEncodeOutput(char c, int charIndex) {
        for(int i = 0; i < numberOfTimesToRepeat; i++) {
            output[charIndex+i] = c;
        }
    }

    @Override
    public void encode() {
        //for every character, there must be numberOfTimesToRepeat character in output;
        int outputLength = input.length*numberOfTimesToRepeat;
        output = new char[input.length*numberOfTimesToRepeat];

        int charIndex = 0;
        for(char c : input) {
            int outputIndex = charIndex*numberOfTimesToRepeat;
            buildEncodeOutput(c, outputIndex);
            charIndex++;
        }
    }

    @Override
    public void decode() {
        //for every numberOfTimesToRepeat characters, one character is extracted into output
        int outputLength = input.length/numberOfTimesToRepeat;
        output = new char[outputLength];

        for(int count = 0; count < outputLength; count++ ) {
            int index = count*numberOfTimesToRepeat;
            output[count] = input[index];
        }
    }
}

class BitLevelEncoderDecoder extends ConcreteEncoderDecoder implements BitLevelConfiguration {

    private final int GROUP_BITS_DECODE;

    public BitLevelEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat) {
        setOperationMode(operationMode);
        setNumberOfTimesToRepeat(numberOfTimesToRepeat);
        GROUP_BITS_DECODE = (GROUP_BITS_ENCODE*numberOfTimesToRepeat) + numberOfTimesToRepeat;
    }

    public BitLevelEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat, char[] input) {
        this(operationMode, numberOfTimesToRepeat);
        setInput(input);
    }

    private void buildEncodeOutput(char[] characters, int charIndex) {
        //For every GROUP_BITS_ENCODE bits, repeat each bit numberOfTimesToRepeat and
        // calculate the final parity bit
        int finalBit = -1;
        for(char c : characters) {
            finalBit = finalBit == -1 ? Converter.getByteEquivalent(c) :
                    ((finalBit^Converter.getByteEquivalent(c))&0x01);
            for(int i = 0; i < numberOfTimesToRepeat; i++) {
                output[charIndex++] = c;
            }
        }
        for(int i = 0; i < numberOfTimesToRepeat; i++) {
            output[charIndex++] = Converter.getCharEquivalent(finalBit);
        }
    }


    @Override
    public void encode() {
        NumberFormatUtil.display(input, 3);
        int numGroups = input.length/GROUP_BITS_ENCODE;
        int outputLength = input.length % GROUP_BITS_ENCODE > 0 ?
                    (numGroups+1)*GROUP_BITS_DECODE : numGroups*GROUP_BITS_DECODE;
        output = new char[outputLength];

        //For every numGroups group of bits, get each group and build the output;
        for(int i = 0; i < numGroups; i++ ) {
            int startIndex = i*GROUP_BITS_ENCODE, endIndex = startIndex+GROUP_BITS_ENCODE;
            int index = i*GROUP_BITS_DECODE;
            buildEncodeOutput(Arrays.copyOfRange(input, startIndex, endIndex), index);
        }

        //remaining bits which dont belong to a group, make it a group with useless bits
        //and then repeat the build process with the new group
        int endIndex = numGroups*GROUP_BITS_ENCODE;
        int difference = input.length-(numGroups*GROUP_BITS_ENCODE);
        if(difference >0) {
            int uselessBitsLength = GROUP_BITS_ENCODE-difference;
            char[] subArray = new char[GROUP_BITS_ENCODE];
            for(int i = 0; i < difference; i++) {
                subArray[i] = input[endIndex + i];
            }
            for(int i = 0; i < uselessBitsLength; i++) {
                subArray[i+difference] = '0';
            }
            buildEncodeOutput(subArray, numGroups*GROUP_BITS_DECODE);
        }
        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);
    }

    private void buildDecodeOutput(char[] characters, int charIndex) {
        for(int i = 0; i < GROUP_BITS_ENCODE; i++) {
            output[charIndex++] = characters[i*numberOfTimesToRepeat];
        }
    }

    @Override
    public void decode() {
        int numGroups = input.length/GROUP_BITS_DECODE;
        int outputLength = numGroups*GROUP_BITS_ENCODE;
        output = new char[outputLength];

        for(int i = 0; i < numGroups; i++) {
            int startIndex = i*GROUP_BITS_DECODE, endIndex = startIndex+GROUP_BITS_DECODE;
            int index = i * GROUP_BITS_ENCODE;
            buildDecodeOutput(Arrays.copyOfRange(input, startIndex, endIndex), index);
        }
        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);
    }
}

class HammingLevelEncoderDecoder extends ConcreteEncoderDecoder implements HammingLevelConfiguration {

    private final int GROUP_BITS_DECODE;
    private int parityBits;
    private int uselessBits;
    private int[] parityBitPositions;


    public HammingLevelEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat) {
        setOperationMode(operationMode);
        setNumberOfTimesToRepeat(numberOfTimesToRepeat);
        setParityBits();
        GROUP_BITS_DECODE = getClosestMultiple(parityBits + GROUP_BITS_ENCODE, 8);
        uselessBits = GROUP_BITS_DECODE-(parityBits+GROUP_BITS_ENCODE);
    }

    public HammingLevelEncoderDecoder(Operation operationMode, int numberOfTimesToRepeat, char[] input) {
        this(operationMode, numberOfTimesToRepeat);
        setInput(input);
    }

    private void setParityBits() {
        parityBits = calculateNumberOfParityBits();
        this.parityBitPositions = new int[parityBits];
        for(int i = 0; i < parityBits; i++)
            parityBitPositions[i] = (int)Math.pow(2, i) - 1;
    }

    private int calculateNumberOfParityBits() {
        int exponent = roundNumberToClosestMultiple(GROUP_BITS_ENCODE, 2);
        return exponent+1;
    }

    private int getClosestMultiple(int num, int base) {
        int result = 1, exponent = 0;
        while(num > result) {
            exponent++;
            result = (int)Math.pow(base, exponent);
        }
        return result;
    }

    private int roundNumberToClosestMultiple(int num, int base) {
        int result = 1, exponent = 0;
        while(num > result) {
            exponent++;
            result = (int)Math.pow(base, exponent);
        }
        return exponent;
    }

    private char getParity(char[] characters, int parityPosition ) {
        int shiftIndex = parityPosition+1;
        int result = -1;
        int index = parityPosition;
        while(index < characters.length) {
            for (int i = index; i < shiftIndex + index; i++) {
                if( i != parityPosition)
                    result = result == -1 ? Converter.getByteEquivalent(characters[i]) :
                        result ^ Converter.getByteEquivalent(characters[i]);
            }
            index += 2*shiftIndex;
        }
        return result == 0 ? '0' : '1';
    }

    private void buildEncodeOutput(char[] characters, int index) {
        char[] encodedCharacters = new char[GROUP_BITS_DECODE];
        for(int i = 0; i < parityBitPositions.length; i++) {
            encodedCharacters[parityBitPositions[i]] = 'x';
        }
        for(int i = 0, j = 0; i < characters.length; i++) {
            while(encodedCharacters[j] != '\u0000')
                j++;
            encodedCharacters[j] = characters[i];
        }
        for(int j = 0; j < uselessBits; j++ )
            encodedCharacters[encodedCharacters.length-1-j] = '0';
        //calculate parityBit based on position
        for(int i = 0; i < parityBits; i++) {
            encodedCharacters[parityBitPositions[i]] = getParity(encodedCharacters, parityBitPositions[i]);
        }
        for(int i = 0; i < encodedCharacters.length; i++)
            output[index++] = encodedCharacters[i];
    }

    @Override
    public void encode() {
        int numGroups = input.length/GROUP_BITS_ENCODE;
        int outputLength = numGroups * GROUP_BITS_DECODE;
        output = new char[outputLength];

        for(int i = 0; i < numGroups; i++) {
            int startIndex = i * GROUP_BITS_ENCODE, endIndex = startIndex + GROUP_BITS_ENCODE;
            int index = i*GROUP_BITS_DECODE;
            buildEncodeOutput(Arrays.copyOfRange(input, startIndex, endIndex), index);
        }
        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);
    }

    private void buildDecodeOutput(char[] characters, int index) {
        char[] encodedCharacters = new char[GROUP_BITS_DECODE];
        char[] decodedCharacters = new char[GROUP_BITS_ENCODE];
        for(int i = 0; i < characters.length; i++)
            encodedCharacters[i] = characters[i];

        for(int i = 0; i < parityBitPositions.length; i++) {
            encodedCharacters[parityBitPositions[i]] = 'x';
        }
        for(int i = 0, j = 0; i < GROUP_BITS_ENCODE; i++) {
            while(encodedCharacters[j] == 'x')
                j++;
            decodedCharacters[i] = encodedCharacters[j];
            j++;
        }
        for(int i = 0; i < GROUP_BITS_ENCODE; i++)
            output[index++] = decodedCharacters[i];
    }

    @Override
    public void decode() {
        int numGroups = input.length/GROUP_BITS_DECODE;
        int outputLength = numGroups*GROUP_BITS_ENCODE;
        output = new char[outputLength];

        for(int i = 0; i < numGroups; i++) {
            int startIndex = i*GROUP_BITS_DECODE, endIndex = startIndex + GROUP_BITS_DECODE;
            int index = i*GROUP_BITS_ENCODE;
            buildDecodeOutput(Arrays.copyOfRange(input, startIndex, endIndex), index);
        }

        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);

    }
}

interface ErrorSimulator extends Container {

    public static final int RANDOM_SEED = 1000;
    public char[] getSymbols();
    public void addErrors();
    public void removeErrors();
}

abstract class ConcreteErrorSimulator extends ConcreteContainer implements ErrorSimulator {

    protected int numberOfCharsForOneError;
    protected Random random;

    protected void setNumberOfCharsForOneError(int numberOfCharsForOneError) {
        this.numberOfCharsForOneError = numberOfCharsForOneError;
    }

}

class SymbolLevelErrorSimulator extends ConcreteErrorSimulator implements SymbolLevelConfiguration {
    public SymbolLevelErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat) {
        setNumberOfCharsForOneError(numberOfCharsForOneError);
        setNumberOfTimesToRepeat(numberOfTimesToRepeat);
        random = new Random(RANDOM_SEED);
        initSymbols();
    }

    public SymbolLevelErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat, char[] input) {
        this(numberOfCharsForOneError, numberOfTimesToRepeat);
        setInput(input);
    }


    private void initSymbols() {
        int counter = 0;
        for(char d : DIGITS)
            SYMBOLS[counter++] = d;
        for(char c : LOWER_CASE)
            SYMBOLS[counter++] = c;
        for(char c : UPPER_CASE)
            SYMBOLS[counter++] = c;
        SYMBOLS[counter] = ' ';
    }

    @Override
    public char[] getSymbols() {
        return SYMBOLS;
    }

    private void buildErroredOutput(char[] characters, int charIndex) {
        int characterToBeChanged = random.nextInt(numberOfCharsForOneError);
        int symbolPickedIndex = random.nextInt(SYMBOLS.length);

        for(int i = 0; i < numberOfCharsForOneError; i++) {
            this.output[charIndex+i] = i != characterToBeChanged ? characters[i] :
                    SYMBOLS[symbolPickedIndex];
        }
    }


    @Override
    public void addErrors() {
        //for every 8 bits, 8 bits are produced in the output, so length remains same
        int outputLength = input.length;
        output = new char[outputLength];

        //for every group of numberOfCharsForOneError, we change one symbol in each group
        int numGroups = input.length/numberOfCharsForOneError;
        for(int count = 0; count < numGroups; count++ ) {
            int startIndex = count*numberOfCharsForOneError,
                    endIndex = startIndex+numberOfCharsForOneError;
            int index = count * numberOfCharsForOneError;
            buildErroredOutput(Arrays.copyOfRange(input,startIndex,endIndex), index);
        }

        //extra symbols beyond the groups simply add them to the output array
        int endCount = numGroups*numberOfCharsForOneError;
        for(;endCount<input.length; endCount++) {
            this.output[endCount] = input[endCount];
        }
    }

    private void buildCorrectOutput(char[] characters, int charIndex) {
        char c1 = characters[0], c2 = '\u0000';
        int countOfChars1 = 1, countOfChars2 = 0;
        boolean isC1 = true;
        for(int i = 1; i < characters.length; i++) {
            if(characters[i] == c1) {
                break;
            } else {
                c2 = characters[i];
                countOfChars2++;
                if(countOfChars2>1) {
                    isC1 = false;
                    break;
                }

            }
        }
        for(int i = 0; i < numberOfCharsForOneError; i++) {
            output[charIndex++] = isC1 == true ? c1 : c2;
        }
    }

    @Override
    public void removeErrors() {
        //for every 8 bits, 8 bits are produced in the output, so length remains same
        int outputLength = input.length;
        output = new char[outputLength];


        for(int count = 0; count < input.length/numberOfCharsForOneError; count++ ) {
            int startIndex = count*numberOfCharsForOneError,
                    endIndex = startIndex+numberOfCharsForOneError;
            int index = count * numberOfCharsForOneError;
            buildCorrectOutput(Arrays.copyOfRange(input,startIndex,endIndex), index);
        }
    }
}

class BitLevelErrorSimulator extends ConcreteErrorSimulator implements BitLevelConfiguration {

    private final int GROUP_BITS_DECODE;

    public BitLevelErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat) {
        setNumberOfCharsForOneError(numberOfCharsForOneError);
        setNumberOfTimesToRepeat(numberOfTimesToRepeat);
        random = new Random(RANDOM_SEED);
        GROUP_BITS_DECODE = GROUP_BITS_ENCODE*numberOfTimesToRepeat+numberOfTimesToRepeat;
    }

    public BitLevelErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat, char[] input) {
        this(numberOfCharsForOneError, numberOfTimesToRepeat);
        setInput(input);
    }

    @Override
    public char[] getSymbols() {
        return SYMBOLS;
    }

    private void buildErroredOutput(char[] characters, int charIndex) {
        //For every numberOfCharsForOneError bits, introduce one error and build the output
        int indexToBeModified = random.nextInt(numberOfCharsForOneError);
        characters[indexToBeModified] = characters[indexToBeModified] == '0' ? '1' : '0';
        for(int i = 0; i < characters.length; i++) {
            output[charIndex+i] = characters[i];
        }
    }

    private void buildCorrectOutput(char[] characters, int charIndex) {
        char[] correctedCharacters = new char[characters.length];
        for(int i = 0; i < characters.length; i++)
            correctedCharacters[i] = characters[i];

        String usefulBits = new String(characters).substring(0, characters.length-numberOfTimesToRepeat);
        char[] parityBits = new char[numberOfTimesToRepeat];
        for(int i = characters.length-numberOfTimesToRepeat, j = 0; i < characters.length; i++, j++)
            parityBits[j] = characters[i];
        String parityBitsString = new String(parityBits);

        if(!Converter.isSequenceIdentical(parityBitsString)) {
            int parityBit = -1;
            for(int i = 0; i < GROUP_BITS_ENCODE; i++) {
                int index = i * numberOfTimesToRepeat;
                parityBit = parityBit == -1 ? Converter.getByteEquivalent(characters[index]) :
                            (parityBit ^ Converter.getByteEquivalent(characters[index]));
            }
            for(int i = 0; i < numberOfTimesToRepeat; i++)
                correctedCharacters[characters.length-numberOfTimesToRepeat+i] =
                        parityBit == 0 ? '0' : '1';
        } else {
            byte parityBitResult = Converter.getByteEquivalent(parityBits[0]);

            int result = parityBitResult, xIndex = -1;
            for(int i = 0; i < GROUP_BITS_ENCODE; i++) {
                int index = i*numberOfTimesToRepeat;
                if(Converter.isSequenceIdentical(
                        usefulBits.substring(index, index+numberOfTimesToRepeat) ))
                    result = result ^ Converter.getByteEquivalent(usefulBits.charAt(index));
                else
                    xIndex = index;
            }
            for(int i = xIndex; i < xIndex+numberOfTimesToRepeat; i++)
                correctedCharacters[i] = result == 0 ? '0' : '1';
        }
        for(char c: correctedCharacters) {
            output[charIndex++] = c;
        }
    }


    @Override
    public void addErrors() {
        //for every bit in input, there needs to be a corresponding bit in output
        int outputLength = input.length;
        output = new char[input.length];

        //get a group of numberOfCharsForOneError bits, and build output
        for(int i = 0; i < input.length/numberOfCharsForOneError; i++) {
            int srcIndex = i*numberOfCharsForOneError,
                    endIndex = srcIndex + numberOfCharsForOneError;
            char[] characters = Arrays.copyOfRange(input, srcIndex, endIndex);
            buildErroredOutput(characters, srcIndex);
        }
        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);
    }

    @Override
    public void removeErrors() {
        System.out.println("removeErrors");
        int numGroups = input.length/GROUP_BITS_DECODE;
        int outputLength = numGroups * GROUP_BITS_ENCODE;
        output = new char[input.length];

        for(int i = 0; i < numGroups; i++) {
            int startIndex = i*GROUP_BITS_DECODE, endIndex = startIndex+GROUP_BITS_DECODE;
            int index = i*GROUP_BITS_DECODE;
            buildCorrectOutput(Arrays.copyOfRange(input, startIndex, endIndex), index);
        }
        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);

    }
}

class HammingLevelErrorSimulator extends ConcreteErrorSimulator implements HammingLevelConfiguration {

    private final int GROUP_BITS_DECODE;
    private int parityBits;
    private int uselessBits;
    private int[] parityBitPositions;


    public HammingLevelErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat) {
        setNumberOfCharsForOneError(numberOfCharsForOneError);
        setNumberOfTimesToRepeat(numberOfTimesToRepeat);
        random = new Random(RANDOM_SEED);
        setParityBits();
        GROUP_BITS_DECODE = getClosestMultiple(parityBits + GROUP_BITS_ENCODE, 8);
        uselessBits = GROUP_BITS_DECODE-(parityBits+GROUP_BITS_ENCODE);
    }

    public HammingLevelErrorSimulator(int numberOfCharsForOneError, int numberOfTimesToRepeat, char[] input) {
        this(numberOfCharsForOneError, numberOfTimesToRepeat);
        setInput(input);
    }

    private void setParityBits() {
        parityBits = calculateNumberOfParityBits();
        this.parityBitPositions = new int[parityBits];
        for(int i = 0; i < parityBits; i++)
            parityBitPositions[i] = (int)Math.pow(2, i) - 1;
    }

    private int calculateNumberOfParityBits() {
        int exponent = roundNumberToClosestMultiple(GROUP_BITS_ENCODE, 2);
        return exponent+1;
    }

    private int getClosestMultiple(int num, int base) {
        int result = 1, exponent = 0;
        while(num > result) {
            exponent++;
            result = (int)Math.pow(base, exponent);
        }
        return result;
    }

    private int roundNumberToClosestMultiple(int num, int base) {
        int result = 1, exponent = 0;
        while(num > result) {
            exponent++;
            result = (int)Math.pow(base, exponent);
        }
        return exponent;
    }


    @Override
    public char[] getSymbols() {
        return new char[0];
    }

    private void buildErroredOutput(char[] characters, int charIndex) {
        //For every numberOfCharsForOneError bits, introduce one error and build the output
        int indexToBeModified = random.nextInt(numberOfCharsForOneError);
        characters[indexToBeModified] = characters[indexToBeModified] == '0' ? '1' : '0';
        for(int i = 0; i < characters.length; i++) {
            output[charIndex+i] = characters[i];
        }
    }


    @Override
    public void addErrors() {
        //for every bit in input, there needs to be a corresponding bit in output
        int outputLength = input.length;
        output = new char[input.length];

        //get a group of numberOfCharsForOneError bits, and build output
        for(int i = 0; i < input.length/numberOfCharsForOneError; i++) {
            int srcIndex = i*numberOfCharsForOneError,
                    endIndex = srcIndex + numberOfCharsForOneError;
            char[] characters = Arrays.copyOfRange(input, srcIndex, endIndex);
            buildErroredOutput(characters, srcIndex);
        }
        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);
    }

    private char getParity(char[] characters, int parityPosition ) {
        int shiftIndex = parityPosition+1;
        int result = -1;
        int index = parityPosition;
        while(index < characters.length) {
            for (int i = index; i < shiftIndex + index; i++) {
                if( i != parityPosition)
                    result = result == -1 ? Converter.getByteEquivalent(characters[i]) :
                            result ^ Converter.getByteEquivalent(characters[i]);
            }
            index += 2*shiftIndex;
        }
        return result == 0 ? '0' : '1';
    }


    private void buildCorrectOutput(char[] characters, int index) {

        for(int i = 0; i < characters.length-uselessBits; i++) {
            output[index+i] = characters[i];
        }
        boolean errorFound = false;
        for(int i = 0; i < uselessBits; i++) {
            int lastIndex = characters.length-1-i;
            if(characters[lastIndex] == '1') {
                output[index+lastIndex] = '0';
                errorFound = true;
            } else
                output[index+lastIndex] = characters[lastIndex];
        }
        if( errorFound == false ) {

            int sumOfErroredParityBits = 0;
            for (int i = 0; i < parityBits; i++) {
                char calculatedParity = getParity(characters, parityBitPositions[i]);
                char existingParity = characters[parityBitPositions[i]];
                if (existingParity != calculatedParity) {
                    sumOfErroredParityBits += parityBitPositions[i] + 1;
                }
            }
            if (sumOfErroredParityBits != 0) {
                char existingChar = characters[sumOfErroredParityBits - 1];
                output[index + sumOfErroredParityBits - 1] = existingChar == '0' ? '1' : '0';
            }
        }
    }

    @Override
    public void removeErrors() {
        output = new char[input.length];
        int numGroups = input.length/GROUP_BITS_DECODE;

        for(int i = 0; i < numGroups; i++ ) {
            int startIndex = i * GROUP_BITS_DECODE, endIndex = startIndex + GROUP_BITS_DECODE;
            int index = i*GROUP_BITS_DECODE;
            buildCorrectOutput(Arrays.copyOfRange(input, startIndex, endIndex), index);
        }
        NumberFormatUtil.display(output, 8);
        NumberFormatUtil.getHexFromBinary(output);
    }
}

enum Operation {
    ENC, DEC, NONE;
}

enum DataManipulationScheme {
    SYMBOL, BIT, HAMMING;
}

enum ProgramMode {
    ENCODE("encode"), SEND_NO_ENCODE("send_no_encode"), SEND("send"), DECODE("decode");
    private String mode;

    ProgramMode(String mode) {
        this.mode = mode;
    }

    public static ProgramMode getMode(String mode) {
        switch(mode) {
            case "encode":
                return ENCODE;
            case "send":
                return SEND;
            case "decode":
                return DECODE;
        }
        return null;
    }
}

class Converter {
    public static char[] preprocessInput(char[] input) {
        char[] output;
        output = NumberFormatUtil.getHexFromDecimal(input);
        output = NumberFormatUtil.getBinaryFromHex(output);
        return output;
    }

    public static char[] postprocessOutput(char[] input) {
        char[] outputBytes = new char[input.length/8];
        int byteIndex = 0;
        for(int i = 0; i < input.length/8; i++) {
            int startIndex = i*8, endIndex=startIndex+8;
            String byteString = new String(Arrays.copyOfRange(input, startIndex, endIndex));
            outputBytes[byteIndex++] = (char)Integer.parseInt( byteString, 2);
        }
        return outputBytes;

    }

    public static byte getByteEquivalent(char c) {
        if(c=='0')
            return 0;
        else if(c=='1')
            return 1;
        return -1;
    }

    public static char getCharEquivalent(int bit) {
        if(bit == 0)
            return '0';
        else if(bit == 1)
            return '1';
        return '\u0000';
    }

    public static boolean isSequenceIdentical(String inputSubstring) {
        char[] characters = inputSubstring.toCharArray();
        char ch = characters[0];
        boolean isIdentical = true;
        for(int i = 1; i < inputSubstring.length(); i++) {
            if(characters[i] != ch) {
                isIdentical = false;
                break;
            }
            ch = characters[i];
        }
        return isIdentical;
    }



}

class NumberFormatUtil {
    public static char[] getHexFromDecimal(char[] decimalChars) {
        char[] characters = new char[decimalChars.length * 2];
        int counter = 0;
        for (char ch : decimalChars) {
            String hex = String.format("%2s", Integer.toHexString((int) ch & 0xFF)).replace(' ', '0');
            for (int i = 0; i < 2; i++)
                characters[counter++] = hex.charAt(i);
        }
        display(characters, 2);
        return characters;
    }

    public static char[] getBinaryFromHex(char[] hexChars) {
        char[] characters = new char[hexChars.length * 4];
        int counter = 0;
        for (char ch : hexChars) {
            int charInt = Integer.parseInt(String.valueOf(ch), 16);
            String binary = String.format("%4s", Integer.toBinaryString(charInt & 0x0F))
                    .replace(' ', '0');
            for (int i = 0; i < 4; i++)
                characters[counter++] = binary.charAt(i);

        }
        display(characters, 8);
        return characters;
    }

    public static char[] getHexFromBinary(char[] binaryChars) {
        StringBuilder builder = new StringBuilder();
        int counter = 0;
        for(int i = 0; i < binaryChars.length/8; i++) {
            int startIndex = i*8, endIndex = startIndex+8;
            String hexStringInBinary = new String(
                    Arrays.copyOfRange(binaryChars, startIndex, endIndex));
            int hexValue = Integer.parseInt(hexStringInBinary, 2);
            String hexString = String.format("%2s", Integer.toHexString(hexValue&0xFF))
                    .replace(' ', '0');
            builder.append(hexString);
        }
        display(builder.toString().toCharArray(), 2);
        return builder.toString().toCharArray();
    }

    public static void display(char[] characters, int groupBits) {
        for (int i = 0; i < characters.length / groupBits; i++) {
            int startIndex = i * groupBits, endIndex = startIndex + groupBits;
            for (int j = startIndex; j < endIndex; j++) {
                System.out.print(characters[j]);
            }
            System.out.print(" ");
        }
        System.out.println();
    }




}
