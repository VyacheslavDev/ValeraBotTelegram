import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.http.util.TextUtils;
import org.json.JSONObject;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.api.methods.groupadministration.KickChatMember;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Created by Vyacheslav on 12/25/2017.
 */
public class Bot extends TelegramLongPollingBot {
    private HashMap<Integer, Integer> mUserAccess = new HashMap<>();
    private HashMap<Long, Integer> mBanList = new HashMap<>();
    private String[] shitCypto = new String[]{"SPRTS", "DIME", "PAC", "FUNK", "SMLY", "IFLT", "PRX", "XIOS", "XIOS"};
    private ArrayList<String> dailyMass = new ArrayList<>();

    private String mApiKey = "490552086:AAGN63CTIMYwu59REI3YF5tnsi4MhpObLPg";
    private String mNameOfBot = "ValeraSecurityManBot";
    int MAX_WRITE_TEXT = 100;

    Message securityWarningMessage = null;
    Calendar calendar = Calendar.getInstance();
    Long mLastChatId;
    boolean isTrue = true;

    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private int TIME_OF_REPEAT_IN_MINUTES = 20;

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (itIsNewMessage(message)) {
            mLastChatId = message.getChatId();
            if (isTrue) {
                executor.scheduleAtFixedRate(periodicTask, 0, TIME_OF_REPEAT_IN_MINUTES, TimeUnit.MINUTES);
                isTrue = false;
            }
            checkAndHandleNewDay(message);
            if (message.getLeftChatMember() != null || message.getNewChatMembers() != null) {
                deleteExtraMessageBots(message);
                return;
            }
            System.out.println("User: [" + message.getFrom().getFirstName() + "]: " + message.getText());

            if (isAdministratorMessage(message) || isTakeAcceess(message)) {

            } else {
                if (securityWarningMessage != null) {
                    deleteExtraMessageBots(securityWarningMessage);
                }
                securityWarningMessage = sendMessageBot(message.getChatId(), setMessageByCount(message));
            }
        }
    }

    Runnable periodicTask = () -> prosseccingZakup();

    private void prosseccingZakup() {
        Double lowerPrice = 0.0;
        try {
            for (String s : shitCypto) {
                ResponseCurrencyData data = getCurrencyData(s);
                lowerPrice = data.getPrice().get(0).get(1);
                for (List<Double> price : data.getPrice()) {
                    if (lowerPrice > price.get(1)) {
                        lowerPrice = price.get(1);
                    }
                }
                Double lastValue = data.getPrice().get(data.getPrice().size() - 1).get(1);
                if (lowerPrice + ((lowerPrice / 100) * 30) > lastValue) {
                    if (!(dailyMass.indexOf(s) > -1)) {
                        sendMessage(new SendMessage(mLastChatId, s + " по ходу скоро можно будет грузить"));
                        dailyMass.add(s);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean itIsNewMessage(Message message) {
        return this.calendar.getTimeInMillis() < (Long.valueOf(message.getDate()) * 1000);
    }

    private void checkAndHandleNewDay(Message message) {
        Calendar calendar = Calendar.getInstance();
        if (!isSameDay(this.calendar, calendar)) {
            this.calendar = Calendar.getInstance();
            dailyMass = new ArrayList<>();
            mUserAccess.clear();
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1 != null && cal2 != null && (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }


    @Override
    public String getBotUsername() {
        return mNameOfBot;
    }

    @Override
    public String getBotToken() {
        return mApiKey;
    }

    private void kickUser(long chatId, int userId) {
        KickChatMember kickChatMember = new KickChatMember(chatId, userId);
        try {
            kickMember(kickChatMember);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String setMessageByCount(Message message) {
        if (mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT + 10) {
            mBanList.put(message.getChatId(), message.getFrom().getId());
            kickUser(message.getChatId(), message.getFrom().getId());
            return message.getFrom().getFirstName() + " удален, и будет думать над свои поведением";
        } else if ((mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT + 8)) {
            return message.getFrom().getFirstName() + " еще одно сообщение, и я удалю тебя из чата";
        } else if ((mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT + 3)) {
            return message.getFrom().getFirstName() + " дождись завтрашнего дня, твой лимит сегодня уже исчерпан";
        } else if (mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT) {
            return "Это было твое последнее сообщение. Лимит закончен";
        }
        return "Эээ спахука";
    }

    private Message sendMessageBot(long ChatId, String text) {
        try {
            return sendMessage(new SendMessage(ChatId, text));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteExtraMessageBots(Message message) {
        DeleteMessage deleteMessage = new DeleteMessage(message.getChatId(), message.getMessageId());
        try {
            deleteMessage(deleteMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isAdministratorMessage(Message message) {
        List<ChatMember> chatMembers = getAdministrator(message);
        for (ChatMember chatMember : chatMembers) {
            if (chatMember.getUser().getId().intValue() == message.getFrom().getId()) {
                return true;
            }
        }
        return false;
    }

    private List<ChatMember> getAdministrator(Message message) {
        GetChatAdministrators getChatAdministrator = new GetChatAdministrators();
        getChatAdministrator.setChatId(message.getChatId());
        try {
            return getChatAdministrators(getChatAdministrator);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return new ArrayList<ChatMember>();
    }

    private boolean isTakeAcceess(Message message) {
        boolean result = false;
        if (mUserAccess.containsKey(message.getFrom().getId())) {
            mUserAccess.put(message.getFrom().getId(), mUserAccess.get(message.getFrom().getId()) + 1);
            System.out.println("User: " + message.getFrom().getFirstName() + ", has: " + mUserAccess.get(message.getFrom().getId()) + " already have written messages");
            if (mUserAccess.get(message.getFrom().getId()) > MAX_WRITE_TEXT) {
                result = false;
                deleteExtraMessageBots(message);
            } else {
                result = true;
            }
        } else {
            mUserAccess.put(message.getFrom().getId(), 1);
            result = true;
        }
        return result;
    }

    public static ResponseCurrencyData getCurrencyData(String nameOfCrypto) throws Exception {
        String url = "http://coincap.io/history/7day/" + nameOfCrypto;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod("GET");
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        //Read JSON response and print
        Gson gson = new GsonBuilder().create();
        ResponseCurrencyData data = gson.fromJson(response.toString(), ResponseCurrencyData.class);
        return data;
    }

}
