def format_device_info(values):
    labels = {
        "manufacturer": "Производитель",
        "model": "Модель",
        "android_version": "Версия Android",
        "sdk": "Уровень API",
    }
    lines = ["Информация об устройстве:"]
    for key, label in labels.items():
        lines.append(f"{label}: {values.get(key, 'неизвестно')}")
    lines.extend(["", "Обработка выполнена встроенным Python.", "Данные остаются на устройстве."])
    return "\n".join(lines)
