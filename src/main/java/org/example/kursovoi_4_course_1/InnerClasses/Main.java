package org.example.kursovoi_4_course_1.InnerClasses;

import org.example.kursovoi_4_course_1.DBClasses.TypeDisplay;
import org.example.kursovoi_4_course_1.DBClasses.User;
import org.example.kursovoi_4_course_1.DBClasses.UserSettings;

public class Main {

    public static void main(String[] args) {

        System.out.println("=== Тест локальных предпочтений (preferences) ===\n");

        // Создаём тестового пользователя
        User testUser = new User();
        testUser.setId(776L);               // произвольный id для теста
        testUser.setLogin("testuser");
        testUser.setName("Тест");
        testUser.setSecond_name("Тестович");

        System.out.println("Создан тестовый пользователь:");
        System.out.println("  ID: " + testUser.getId());
        System.out.println("  Login: " + testUser.getLogin());
        System.out.println("  Имя: " + testUser.getName() + " " + testUser.getSecond_name());
        System.out.println();

        // Проверяем, есть ли уже настройки
        System.out.println("Попытка загрузить существующие локальные настройки...");
        testUser.loadLocalPreferences();

        // Смотрим, что получилось
        if (testUser.getUser_settings() != null) {
            UserSettings settings = testUser.getUser_settings();
            System.out.println("Успешно загружены настройки:");
            System.out.println("  TypeDisplay: " + settings.getTypeDisplay());
            System.out.println("  UpdatedAt:   " + settings.getUpdatedAt());
            if (settings.getModelBboxId() != null) {
                System.out.println("  ModelBBox ID: " + settings.getModelBboxId());
            }
            if (settings.getModelKeypointsId() != null) {
                System.out.println("  ModelKeypoints ID: " + settings.getModelKeypointsId());
            }
        } else {
            System.out.println("Настройки для пользователя не найдены в файле (или файл пустой).");
            System.out.println("Будет использоваться значение по умолчанию.");
        }

        System.out.println("\nТекущее значение TypeDisplay в объекте: " +
                (testUser.getUser_settings() != null ? testUser.getUser_settings().getTypeDisplay() : "не задано"));

        // Меняем значение (имитация выбора в интерфейсе)
        TypeDisplay newType = TypeDisplay.KEYPOINTS;
        System.out.println("\nМеняем тип отображения на: " + newType);

        testUser.saveTypeDisplayToLocalPreferences(newType);

        System.out.println("Сохранение выполнено.");

        // Проверяем, что изменилось в объекте
        if (testUser.getUser_settings() != null) {
            System.out.println("После сохранения в объекте:");
            System.out.println("  TypeDisplay: " + testUser.getUser_settings().getTypeDisplay());
            System.out.println("  UpdatedAt:   " + testUser.getUser_settings().getUpdatedAt());
        }

        // Дополнительно: ещё раз загружаем из файла (проверка целостности)
        System.out.println("\nПовторная загрузка из файла для проверки...");
        testUser.loadLocalPreferences();

        if (testUser.getUser_settings() != null) {
            System.out.println("После повторной загрузки:");
            System.out.println("  TypeDisplay: " + testUser.getUser_settings().getTypeDisplay());
        }

        System.out.println("\n=== Тест завершён ===");
        System.out.println("Файл сохранён по пути:");
        System.out.println("  " + User.getPreferencesFilePath().toAbsolutePath());
        System.out.println();
        System.out.println("Можешь открыть этот файл в текстовом редакторе и посмотреть содержимое.");
        System.out.println("Для другого пользователя создай нового с другим id и повтори тест.");
    }
}
