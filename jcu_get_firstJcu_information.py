import sqlite3
from selenium import webdriver
from selenium.webdriver.firefox.service import Service
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.firefox import GeckoDriverManager
import time
from bs4 import BeautifulSoup
from datetime import datetime
import pytz
import requests
from dotenv import load_dotenv
import os
import sys
import re

# Load environment variables like a pro! 🧙‍♂️
load_dotenv()

# Database setup (where the magic happens ✨)
DB_NAME = 'jcu_assignments.db'

# Telegram Bot Token (shh, it's a secret! 🤫)
TELEGRAM_BOT_TOKEN = os.getenv('TELEGRAM_BOT_TOKEN')

if len(sys.argv) > 1:
    USERNAME = sys.argv[1]
else:
    USERNAME = ""


def setup_database():
    conn = sqlite3.connect(DB_NAME)
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS class_schedules
                 (id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id INTEGER,
                  course_name TEXT,
                  course_code TEXT,
                  start_time TEXT,
                  end_time TEXT,
                  location TEXT,
                  status TEXT,
                  FOREIGN KEY (user_id) REFERENCES users(user_id))''')
    conn.commit()
    conn.close()


def get_all_users():
    conn = sqlite3.connect(DB_NAME)
    c = conn.cursor()
    if USERNAME:
        c.execute("SELECT user_id, username, password FROM users WHERE username = ?", (USERNAME,))
    else:
        c.execute("SELECT user_id, username, password FROM users")
    users = c.fetchall()
    conn.close()
    return users


def update_class_schedules(user_id, courses):
    conn = sqlite3.connect(DB_NAME)
    c = conn.cursor()

    # Get existing schedules (like a time traveler! ⏰)
    c.execute(
        "SELECT id, course_name, course_code, start_time, end_time, location, status FROM class_schedules WHERE user_id = ?",
        (user_id,))
    existing_schedules = c.fetchall()

    new_schedules = []
    deleted_schedules = []
    updated_schedules = []

    # Convert existing schedules to a set (for super-fast comparisons! 🚀)
    existing_set = set((course_code, start_time, end_time, location, status) for
                       _, _, course_code, start_time, end_time, location, status in existing_schedules)

    # Process new schedules (time to shake things up! 🎭)
    new_set = set()
    for course in courses:
        course_name = course['course_name']
        course_code = course['course_code']
        for detail in course['details']:
            start_time = detail['start_time']
            end_time = detail['end_time']
            location = detail['location']
            status = detail['status']

            new_set.add((course_code, start_time, end_time, location, status))

            if (course_code, start_time, end_time, location, status) not in existing_set:
                c.execute("""INSERT INTO class_schedules (user_id, course_name, course_code, start_time, end_time, location, status)
                             VALUES (?, ?, ?, ?, ?, ?, ?)""",
                          (user_id, course_name, course_code, start_time, end_time, location, status))
                new_schedules.append((course_name, course_code, start_time, end_time, location, status))

    # Handle deleted schedules (goodbye, old friends! 👋)
    for schedule in existing_schedules:
        schedule_id, course_name, course_code, start_time, end_time, location, status = schedule
        if (course_code, start_time, end_time, location, status) not in new_set:
            c.execute("DELETE FROM class_schedules WHERE id = ?", (schedule_id,))
            deleted_schedules.append((course_name, course_code, start_time, end_time, location, status))

    conn.commit()
    conn.close()

    return new_schedules, deleted_schedules, updated_schedules


def parse_html_and_extract_info(html_content):
    soup = BeautifulSoup(html_content, 'html.parser')

    # Extract course info (like a data detective! 🕵️‍♂️)
    courses = []
    course_elements = soup.find_all('li', class_='flex align-items-center mb-3 ml-3')
    for course_element in course_elements:
        # Extract course title (the star of the show! 🌟)
        title_tag = course_element.find_previous_sibling('li', class_='flex align-items-center mb-1').find('span',
                                                                                                           class_='text-900 font-medium text-xl m-2')
        if title_tag:
            course_title = title_tag.get_text(strip=True)
        else:
            course_title = 'Mystery Course 🎭'

        # Extract course code (the secret identity! 🦸‍♂️)
        code_tag = course_element.find_previous_sibling('li', class_='flex align-items-center mb-2 ml-3')
        if code_tag:
            course_code = code_tag.find('span', class_='text-600 text-l mb-2').get_text(strip=True)
        else:
            course_code = 'Unknown Code 🔍'

        # Extract course details (the juicy bits! 🍊)
        details_div = course_element.find('div', class_='flex flex-wrap')
        if details_div:
            detail_labels = details_div.find_all('label', class_='ui-outputlabel ui-widget')
            details = []
            for label in detail_labels:
                detail_div = label.find('div')
                if detail_div:
                    texts = detail_div.get_text(separator='<br/>').split('<br/>')
                    date_str = texts[0].strip()
                    time_range = texts[1].strip()
                    location = texts[2].strip()

                    # Parse date and time (time wizardry! ⏳)
                    date = datetime.strptime(f"{date_str}-{datetime.now().year}", "%d-%b-%Y")
                    start_time, end_time = time_range.split('-')

                    # Format start and end times (making time look pretty! 💅)
                    start_datetime = date.replace(hour=int(start_time[:2]), minute=int(start_time[2:]))
                    end_datetime = date.replace(hour=int(end_time[:2]), minute=int(end_time[2:]))

                    formatted_start_time = start_datetime.strftime("%Y-%m-%d %H:%M")
                    formatted_end_time = end_datetime.strftime("%Y-%m-%d %H:%M")

                    # Determine attendance status (the moment of truth! 😱)
                    class_names = detail_div.get('class', [])
                    status = "Unknown"
                    if 'border-green-500' in class_names:
                        status = "Completed"
                    elif 'border-red-500' in class_names:
                        status = "Absent"
                    elif 'bg-gray-700' in class_names:
                        status = "Planned"

                    details.append({
                        'start_time': formatted_start_time,
                        'end_time': formatted_end_time,
                        'location': location,
                        'status': status
                    })

            courses.append({
                'course_name': course_title,
                'course_code': course_code,
                'details': details
            })

    return courses


def login_jcu(username, password):
    # Initialize browser driver (vroom vroom! 🏎️)
    service = Service(GeckoDriverManager().install())
    options = webdriver.FirefoxOptions()
    options.add_argument('--headless')  # Ninja mode activated! 🥷
    driver = webdriver.Firefox(service=service, options=options)

    try:
        driver.get('https://studentfirst.jcu.edu.sg/faces/login.xhtml')  # Off to JCU we go! 🏫
        time.sleep(2)
        
        # Wait for input fields (patience is a virtue! 🧘‍♂️)
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.XPATH, "//input[contains(@id, 'sf_loginform_loginid')]"))
        )
        WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.XPATH, "//input[contains(@id, 'sf_loginform_password')]"))
        )

        # Find input fields (like a web treasure hunt! 🗺️)
        username_field = driver.find_element(By.XPATH, "//input[contains(@id, 'sf_loginform_loginid')]")
        password_field = driver.find_element(By.XPATH, "//input[contains(@id, 'sf_loginform_password')]")

        username_field.send_keys(username)
        password_field.send_keys(password)

        # Click login button (fingers crossed! 🤞)
        login_button = driver.find_element(By.CLASS_NAME, 'login_button')
        login_button.click()

        # Wait for page to load (drumroll, please... 🥁)
        WebDriverWait(driver, 10).until(
            EC.text_to_be_present_in_element((By.TAG_NAME, "body"), "List of classes and schedule for next 8 days")
        )

        # Get current URL and page content (data harvest time! 🌾)
        current_url = driver.current_url
        html_content = driver.page_source

        # Parse HTML and extract info (time to work some magic! ✨)
        courses = parse_html_and_extract_info(html_content)

        return courses, "Login successful! 🎉", html_content, current_url
    except Exception as e:
        print(f"Oops! Something went wrong: {e}")
        return None, f"Login failed (sad trombone 🎺): {e}", None, None
    finally:
        driver.quit()


def send_telegram_message(user_id, message):
    url = f"https://api.telegram.org/bot{TELEGRAM_BOT_TOKEN}/sendMessage"
    max_length = 4096  # Telegram's message size limit (don't anger the bot! 🤖)

    if len(message) <= max_length:
        data = {
            "chat_id": user_id,
            "text": message,
            "parse_mode": "HTML"
        }
        response = requests.post(url, data=data)
        print(response.json())
        return response.json()
    else:
        # If message is too long, split it (like a message ninja! 🥷)
        chunks = [message[i:i + max_length] for i in range(0, len(message), max_length)]
        responses = []
        for chunk in chunks:
            data = {
                "chat_id": user_id,
                "text": chunk,
                "parse_mode": "HTML"
            }
            response = requests.post(url, data=data)
            print(response.json())
            responses.append(response.json())
        return responses


def process_user(user_id, username, password):
    courses, status, html_content, current_url = login_jcu(username, password)

    if status.startswith("Login successful"):
        new_schedules, deleted_schedules, updated_schedules = update_class_schedules(user_id, courses)

        # Process updated schedules (time for some schedule juggling! 🤹‍♂️)
        real_new_schedules = []
        real_deleted_schedules = []
        real_updated_schedules = []

        new_dict = {(s[0], s[1], s[2]): s for s in new_schedules}
        deleted_dict = {(s[0], s[1], s[2]): s for s in deleted_schedules}

        for key in set(new_dict.keys()) & set(deleted_dict.keys()):
            new = new_dict[key]
            old = deleted_dict[key]
            if new[4] != old[4]:
                real_updated_schedules.append((new, "Location", old[4], new[4]))
            if new[5] != old[5]:
                real_updated_schedules.append((new, "Status", old[5], new[5]))
            del new_dict[key]
            del deleted_dict[key]

        real_new_schedules = list(new_dict.values())
        real_deleted_schedules = list(deleted_dict.values())

        notification = ""
        if real_updated_schedules:
            notification += f"<b>🔄 Updated {len(real_updated_schedules)} class schedules:</b>\n\n"
            for schedule, change_type, old_value, new_value in real_updated_schedules:
                notification += f"Course: {schedule[0]} 📚\n"
                notification += f"Code: {schedule[1]} 🔢\n"
                notification += f"Start: {schedule[2]} 🏁\n"
                notification += f"End: {schedule[3]} 🏁\n"

                def get_emoji(value):
                    if value == "Planned":
                        return "⚪️"
                    elif value == "Absent":
                        return "🔴"
                    elif value == "Completed":
                        return "🟢"
                    elif value == "Unknown":
                        return "🟡"
                    else:
                        return "⚫️"

                old_emoji = get_emoji(old_value)
                new_emoji = get_emoji(new_value)

                notification += f"{change_type} changed from <b>{old_emoji} {old_value}</b> to <b>{new_emoji} {new_value}</b>\n\n"

        print(f"User {username} processed successfully:")
        print(notification)
        return True, notification
    else:
        print(f"User {username} login failed: {status}")
        return False, status


def main():
    setup_database()
    users = get_all_users()

    for user_id, username, password in users:
        success, message = process_user(user_id, username, password)
        if success:
            print(f"Class schedules updated for user {username} 🎉")
            if message:
                send_telegram_message(user_id, f"<b>🚀 Class Schedule Update Complete!</b>\n\n{message}")
        else:
            print(f"Error processing user {username}: {message} 😢")
            send_telegram_message(user_id, f"<b>❌ Error Updating Class Schedules</b>\n\n{message}")

        # Add a little delay between users (don't anger the server gods! 🙏)
        time.sleep(5)


if __name__ == "__main__":
    main()
