@Grapes(
    @Grab(group='net.sourceforge.nekohtml', module='nekohtml', version='1.9.21')
)
import java.text.SimpleDateFormat
import java.text.DateFormat
import groovy.time.TimeCategory

// configuration parameters
def uri = "https://hvbrandenburg-handball.liga.nu/cgi-bin/WebObjects/nuLigaHBDE.woa/wa/teamPortrait?teamtable=1713873&pageState=vorrunde&championship=HVBrandenburg+2020+%2F+2021&group=263701"
def team = "SV Motor Babelsberg"
def league = "Landesliga Männer Mitte"
def calendarName = "Landesliga_Männer_Mitte_2020_21.ics"

// create date formats
TimeZone utcTimeZone = TimeZone.getTimeZone("UTC")
isoDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
isoDateFormat.setTimeZone(utcTimeZone)

TimeZone berlinTimeZone = TimeZone.getTimeZone("Europe/Berlin")
berlinDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss")
berlinDateFormat.setTimeZone(berlinTimeZone)

// initialise parser
def parser = new org.cyberneko.html.parsers.SAXParser()
def page = new XmlSlurper(parser).parse(uri)

def table = page.'**'.find {
    it.'@id'=='content-row2'
}.TABLE.TBODY

// parse nuliga website
def games = []
def previousDate = ""

table.TR.each { row ->
    def game = [:]
    def isValidGame = false

    // iterate over matches and parse entries
    row.children().eachWithIndex { entry, i ->
        if (entry.name() != "TH") {
            isValidGame = true
            switch (i) {
                case 1 : game.date = (entry.text().trim().length()>1 ? entry.text().trim() : previousDate); previousDate = game.date; break;
                case 2 : game.time = entry.text().trim(); game.time = game.time.size() > 5 ? game.time.substring(0, 5) : game.time; game.description = entry.@title; break;
                case 3 : game.location = "${entry.text().trim()}: ${entry.SPAN.@title}"; break;
                case 4 : game.number = entry.text().trim(); break;
                case 5 : game.home = entry.text().trim(); break;
                case 6 : game.guest = entry.text().trim(); break;
            }
        }
    }

    // discard matches of other teams
    if (isValidGame && team && !game.home.contains(team) && !game.guest.contains(team)) {
        isValidGame = false
    }

    // discard "spielfrei" matches
    if (isValidGame && (game.home.contains("spielfrei") || game.guest.contains("spielfrei"))) {
        isValidGame = false
    }

    // convert time to correct format for ICS calendar
    if (isValidGame) {
        game.start = convertTime("${game.date} ${game.time}", false)
        game.end = convertTime("${game.date} ${game.time}", true)

        games.add(game)
    }
}

// helper function to convert time
def convertTime(time, isEnd) {
    Date date = new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(time)

    if (isEnd) {
        Integer.metaClass.mixin TimeCategory
        Date.metaClass.mixin TimeCategory
        date = date + 2.hours
    }

    String berlinTime = berlinDateFormat.format(date)
}

def now = isoDateFormat.format(new Date())

// write ICS calendar file
new File(calendarName).withWriter { out ->
    out.write("BEGIN:VCALENDAR\r\n")
    out.write("VERSION:2.0\r\n")
    out.write("PRODID:-//Ingmar Rötzler//NONSGML nuLiga Calendar//EN\r\n")
    out.write("METHOD:PUBLISH\r\n")
    out.write("X-WR-TIMEZONE:Europe/Berlin\r\n")
    out.write("CALSCALE:GREGORIAN\r\n")

    out.write("BEGIN:VTIMEZONE\r\n")
    out.write("TZID:Europe/Berlin\r\n")
    out.write("BEGIN:DAYLIGHT\r\n")
    out.write("TZOFFSETFROM:+0100\r\n")
    out.write("RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n")
    out.write("DTSTART:19810329T020000\r\n")
    out.write("TZNAME:GMT+2\r\n")
    out.write("TZOFFSETTO:+0200\r\n")
    out.write("END:DAYLIGHT\r\n")
    out.write("BEGIN:STANDARD\r\n")
    out.write("TZOFFSETFROM:+0200\r\n")
    out.write("RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n")
    out.write("DTSTART:19961027T030000\r\n")
    out.write("TZNAME:GMT+1\r\n")
    out.write("TZOFFSETTO:+0100\r\n")
    out.write("END:STANDARD\r\n")
    out.write("END:VTIMEZONE\r\n")

    games.each { game ->
        out.write("BEGIN:VEVENT\r\n")
        out.write("UID:${game.number}\r\n")
        out.write("ORGANIZER:\r\n")
        out.write("LOCATION:${game.location}\r\n")
        out.write("SUMMARY:${game.home} - ${game.guest}\r\n")
        out.write("DESCRIPTION:${[league, game.description.toString().replace(',', '\\,')].minus(['']).join('\\, ')}\r\n")
        out.write("CLASS:PUBLIC\r\n")
        out.write("DTSTART;TZID=Europe/Berlin:${game.start}\r\n")
        out.write("DTEND;TZID=Europe/Berlin:${game.end}\r\n")
        out.write("DTSTAMP:${now}\r\n")
        out.write("END:VEVENT\r\n")
    }

    out.write("END:VCALENDAR\r\n")
}

println "Calendar file '${calendarName}' created successfully for team ${team}."
