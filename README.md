# Modern Rotation Control

This is a clone of [Crape Myrtle's Rotation Control](https://play.google.com/store/apps/details?id=org.crape.rotationcontrol&hl=en) rewrote from scratch, but heavily inspired (from design to features).

The only major difference is that I do not include the app's icon at the very left of the notification as starting Android 14, custom notification layouts are heavily restricted.

<table>
  <thead>
    <tr>
      <td>Settings</td>
      <td>Notification</td>
      <td>Smart Tile Icon</td>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/464676ff-1056-4f76-a206-7e3beba11dee" width="300px" /></td>
      <td><img src="https://github.com/user-attachments/assets/ac7f491c-018a-45f1-9d0f-a2831fd0e0b5" width="300px" /></td>
      <td><img src="https://github.com/user-attachments/assets/ee38841a-35a5-49bc-988a-d2420f1a8de2" width="300px" /></td>
    </tr>
  </tbody>
</table>

## Reason

I mostly did it for myself as a fun challenge, but I will likely be the only one using it.

## Key Differences

### Features

- The notification is optional and a Quick Settings tile is available.
- The mode/guard is editable in the configuration page.
- The mode is reapplied each time the screen is unlocked (for more consistent use).
- A button to reapply the current mode, as some applications can break when the orientation change happens in the background.

### Changes compared to Crape Myrtle's version

- The application icon to the left of the notification has been removed.
