from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions
import time
b = webdriver.Chrome()
b.get("https://chat.openai.com/")
WebDriverWait(b, 300, 1).until(expected_conditions.presence_of_element_located((By.ID, "prompt-textarea")))
#print("检测到登录成功")
cmd = input()
while not cmd == "exit":
    try:
        lst = cmd.strip().split(" ")
        b.find_element(by=By.XPATH, value="/html/body/div[1]/div[1]/div[2]/main/div[1]/div[2]/form/div/div[2]/div/div/span/input").send_keys(lst[0])
        b.find_element(value="prompt-textarea").send_keys(lst[1])
        while not b.find_element(by=By.XPATH, value="//form/div/div[2]/div/button").is_enabled():
            time.sleep(1)
        b.find_element(by=By.XPATH, value="//form/div/div[2]/div/button").click()
        WebDriverWait(b, 60, 1).until(expected_conditions.presence_of_element_located((By.CSS_SELECTOR, ".ml-auto > .icon-sm")))
        print("~" + b.find_element(by=By.CSS_SELECTOR, value=".markdown > p").text)
        b.find_element(by=By.XPATH, value="//button[2]").click()
        b.find_element(by=By.CSS_SELECTOR, value=".btn-danger > .flex").click()
    except Exception as e:
        print(e)
    finally:
        cmd = input()
b.close()