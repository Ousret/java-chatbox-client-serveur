package html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ChatBox {

    private static final String DOM_BASE_PATH = "/Users/Ousret/IdeaProjects/imie-java-projet1/src/main/resources/html/chatbox.html";
    private static final String DOM_MESSAGE_PATH = "/Users/Ousret/IdeaProjects/imie-java-projet1/src/main/resources/html/message.html";

    private static String RAW_MESSAGE_HTML = "";
    private static String RAW_CONTENT_BASE = "";

    private String raw;

    private Document document;

    private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;

    public ChatBox() throws IOException
    {
        if (ChatBox.RAW_CONTENT_BASE.isEmpty())
        {
            ChatBox.RAW_CONTENT_BASE = this.chargerBase(ChatBox.DOM_BASE_PATH);
            ChatBox.RAW_MESSAGE_HTML = this.chargerBase(ChatBox.DOM_MESSAGE_PATH);
        }

        this.simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        this.clear();
    }

    /**
     * Charge les lignes d'un fichier et rend un String
     * @param unCheminRelatif Le chemin relatif ou complet de la ressource
     * @return String
     * @throws IOException
     */
    private String chargerBase(String unCheminRelatif) throws IOException
    {
        return new String(Files.readAllBytes(Paths.get(unCheminRelatif)), StandardCharsets.UTF_8);
    }

    /**
     * Ajouter un message à la file du chat (HTML)
     * @param unAuteur L'auteur du message
     * @param unMessage Le corps du message
     */
    public void addMessage(String unAuteur, String unMessage)
    {
        this.calendar = Calendar.getInstance();
        this.document.select("ul").first().append(String.format(ChatBox.RAW_MESSAGE_HTML, unAuteur, this.simpleDateFormat.format(this.calendar.getTime()), unMessage));
        this.raw = this.document.outerHtml();
    }

    /**
     * Récupère le DOM sous format texte
     * @return String
     */
    public String getRaw()
    {
        return this.raw;
    }

    /**
     * Réinitialise le contenu à l'initial
     */
    public void clear()
    {
        this.document = Jsoup.parse(ChatBox.RAW_CONTENT_BASE);
        this.raw = this.document.outerHtml();
    }

}
